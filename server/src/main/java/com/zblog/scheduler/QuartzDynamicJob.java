package com.zblog.scheduler;

import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class QuartzDynamicJob extends QuartzJobBean {

  @Autowired private ScheduledJobService scheduledJobService;

  @Override
  protected void executeInternal(JobExecutionContext context) {
    long jobId = context.getMergedJobDataMap().getLong("jobId");
    scheduledJobService.executeScheduled(jobId);
  }
}
