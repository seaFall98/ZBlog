package com.zblog.scheduler;

import com.zblog.common.exception.BusinessException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobHandlerRegistry {

  private final Map<String, ScheduledJobHandler> handlers;

  public ScheduledJobHandlerRegistry(List<ScheduledJobHandler> handlers) {
    this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(ScheduledJobHandler::name, Function.identity()));
  }

  public ScheduledJobHandler require(String name) {
    ScheduledJobHandler handler = handlers.get(name);
    if (handler == null) {
      throw new BusinessException(400, "Unknown scheduled job handler", HttpStatus.BAD_REQUEST);
    }
    return handler;
  }

  public List<Map<String, Object>> list() {
    return handlers.values().stream()
        .sorted(Comparator.comparing(ScheduledJobHandler::name))
        .map(
            handler ->
                Map.<String, Object>of(
                    "name",
                    handler.name(),
                    "description",
                    handler.description(),
                    "default_parameters",
                    handler.defaultParameters()))
        .toList();
  }
}
