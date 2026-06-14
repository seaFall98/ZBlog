package com.zblog.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledJobService implements ApplicationRunner {

  private static final String GROUP = "zblog-scheduled-jobs";

  private final ScheduledJobMapper mapper;
  private final ScheduledJobHandlerRegistry handlerRegistry;
  private final ObjectMapper objectMapper;
  private final Scheduler scheduler;

  public ScheduledJobService(
      ScheduledJobMapper mapper,
      ScheduledJobHandlerRegistry handlerRegistry,
      ObjectMapper objectMapper,
      Scheduler scheduler) {
    this.mapper = mapper;
    this.handlerRegistry = handlerRegistry;
    this.objectMapper = objectMapper;
    this.scheduler = scheduler;
  }

  @Override
  public void run(ApplicationArguments args) {
    for (Map<String, Object> row : mapper.listJobs(1000, 0)) {
      if (Boolean.TRUE.equals(row.get("enabled"))) {
        schedule(mapJob(row));
      }
    }
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    return new PageResponse<>(mapper.listJobs(pageSize, offset).stream().map(this::view).toList(), mapper.countJobs(), page, pageSize);
  }

  public List<Map<String, Object>> handlers() {
    return handlerRegistry.list();
  }

  public PageResponse<Map<String, Object>> logs(long jobId, int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    return new PageResponse<>(mapper.listLogs(jobId, pageSize, offset).stream().map(this::viewLog).toList(), mapper.countLogs(jobId), page, pageSize);
  }

  @Transactional
  public Map<String, Object> update(long id, Map<String, Object> request) {
    ScheduledJob existing = findJob(id);
    String cron = textOrDefault(request, "cron_expression", existing.cronExpression());
    String parameters = parametersJson(request.getOrDefault("parameters", existing.parameters()));
    validate(existing.handlerName(), cron, parameters);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("id", id);
    params.put("cronExpression", cron);
    params.put("parameters", parameters);
    params.put("enabled", booleanOrDefault(request.get("enabled"), existing.enabled()));
    mapper.updateJob(params);
    unschedule(id);
    ScheduledJob updated = findJob(id);
    if (updated.enabled()) {
      schedule(updated);
    }
    return view(updated);
  }

  @Transactional
  public Map<String, Object> setEnabled(long id, boolean enabled) {
    mapper.updateEnabled(id, enabled);
    if (enabled) {
      schedule(findJob(id));
    } else {
      unschedule(id);
    }
    return view(findJob(id));
  }

  public Map<String, Object> runNow(long id) {
    return execute(id, "manual");
  }

  public void executeScheduled(long id) {
    execute(id, "quartz");
  }

  private Map<String, Object> execute(long id, String source) {
    ScheduledJob job = findJob(id);
    Map<String, Object> params = parseParameters(job.parameters());
    long logId = startLog(job);
    LocalDateTime started = LocalDateTime.now();
    try {
      String message = handlerRegistry.require(job.handlerName()).execute(params);
      finishLog(logId, "success", source + ": " + message, started);
      mapper.updateLastRunAt(id);
    } catch (RuntimeException exception) {
      finishLog(logId, "failed", source + ": " + exception.getMessage(), started);
      throw exception;
    }
    return viewLog(mapper.listLogs(id, 1, 0).getFirst());
  }

  private void validate(String handlerName, String cronExpression, String parameters) {
    handlerRegistry.require(handlerName);
    try {
      CronExpression.validateExpression(cronExpression);
      parseParameters(parameters);
    } catch (RuntimeException | java.text.ParseException exception) {
      throw new BusinessException(400, "Invalid scheduled job configuration", HttpStatus.BAD_REQUEST);
    }
  }

  private void schedule(ScheduledJob job) {
    validate(job.handlerName(), job.cronExpression(), job.parameters());
    try {
      unschedule(job.id());
      JobDataMap dataMap = new JobDataMap();
      dataMap.put("jobId", job.id());
      JobDetail jobDetail =
          JobBuilder.newJob(QuartzDynamicJob.class).withIdentity(jobKey(job.id())).setJobData(dataMap).storeDurably().build();
      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity(triggerKey(job.id()))
              .forJob(jobDetail)
              .withSchedule(CronScheduleBuilder.cronSchedule(job.cronExpression()).withMisfireHandlingInstructionDoNothing())
              .build();
      scheduler.scheduleJob(jobDetail, trigger);
    } catch (SchedulerException exception) {
      throw new BusinessException(500, "Failed to schedule job", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void unschedule(long id) {
    try {
      if (scheduler.checkExists(jobKey(id))) {
        scheduler.deleteJob(jobKey(id));
      }
    } catch (SchedulerException exception) {
      throw new BusinessException(500, "Failed to unschedule job", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private ScheduledJob findJob(long id) {
    return mapper.rowsById(id).stream()
        .findFirst()
        .map(this::mapJob)
        .orElseThrow(() -> new BusinessException(404, "Scheduled job not found", HttpStatus.NOT_FOUND));
  }

  private ScheduledJob mapJob(Map<String, Object> row) {
    return new ScheduledJob(
        number(row, "id"),
        text(row, "name"),
        text(row, "handler_name"),
        text(row, "cron_expression"),
        text(row, "parameters"),
        Boolean.TRUE.equals(row.get("enabled")),
        text(row, "description"),
        localDateTime(row.get("last_run_at")),
        localDateTime(row.get("created_at")),
        localDateTime(row.get("updated_at")));
  }

  private Map<String, Object> view(Map<String, Object> row) {
    return view(mapJob(row));
  }

  private Map<String, Object> view(ScheduledJob job) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", job.id());
    view.put("name", job.name());
    view.put("handler_name", job.handlerName());
    view.put("cron_expression", job.cronExpression());
    view.put("parameters", parseParameters(job.parameters()));
    view.put("enabled", job.enabled());
    view.put("description", job.description());
    view.put("last_run_at", string(job.lastRunAt()));
    view.put("created_at", string(job.createdAt()));
    view.put("updated_at", string(job.updatedAt()));
    return view;
  }

  private Map<String, Object> viewLog(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("job_id", row.get("job_id"));
    view.put("job_name", row.get("job_name"));
    view.put("handler_name", row.get("handler_name"));
    view.put("status", row.get("status"));
    view.put("message", row.get("message"));
    view.put("started_at", string(localDateTime(row.get("started_at"))));
    view.put("finished_at", string(localDateTime(row.get("finished_at"))));
    view.put("duration_ms", row.get("duration_ms"));
    return view;
  }

  private long startLog(ScheduledJob job) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("jobId", job.id());
    params.put("jobName", job.name());
    params.put("handlerName", job.handlerName());
    params.put("status", "running");
    mapper.insertLog(params);
    return number(params, "id");
  }

  private void finishLog(long id, String status, String message, LocalDateTime started) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("id", id);
    params.put("status", status);
    params.put("message", message);
    params.put("durationMs", Duration.between(started, LocalDateTime.now()).toMillis());
    mapper.finishLog(params);
  }

  private JobKey jobKey(long id) {
    return new JobKey("job-" + id, GROUP);
  }

  private TriggerKey triggerKey(long id) {
    return new TriggerKey("trigger-" + id, GROUP);
  }

  private Map<String, Object> parseParameters(String json) {
    try {
      return objectMapper.readValue(json == null || json.isBlank() ? "{}" : json, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(400, "Invalid job parameters", HttpStatus.BAD_REQUEST);
    }
  }

  private String parametersJson(Object value) {
    try {
      if (value instanceof String text) {
        parseParameters(text);
        return text;
      }
      return objectMapper.writeValueAsString(value == null ? Map.of() : value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(400, "Invalid job parameters", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean booleanOrDefault(Object value, boolean fallback) {
    return value instanceof Boolean bool ? bool : fallback;
  }

  private String textOrDefault(Map<String, Object> request, String key, String fallback) {
    String value = text(request, key);
    return value.isBlank() ? fallback : value;
  }

  private String text(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? "" : value.toString();
  }

  private long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }

  private LocalDateTime localDateTime(Object value) {
    if (value instanceof LocalDateTime dateTime) {
      return dateTime;
    }
    return null;
  }

  private String string(LocalDateTime value) {
    return value == null ? null : value.toString().replace('T', ' ');
  }
}
