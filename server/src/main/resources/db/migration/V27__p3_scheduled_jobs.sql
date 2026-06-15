create table if not exists scheduled_jobs (
  id bigserial primary key,
  name varchar(120) not null,
  handler_name varchar(120) not null,
  cron_expression varchar(120) not null,
  parameters text not null default '{}',
  enabled boolean not null default true,
  description varchar(500),
  last_run_at timestamp,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create unique index if not exists uk_scheduled_jobs_handler_name on scheduled_jobs(handler_name);
create index if not exists idx_scheduled_jobs_enabled on scheduled_jobs(enabled);

create table if not exists scheduled_job_logs (
  id bigserial primary key,
  job_id bigint not null,
  job_name varchar(120) not null,
  handler_name varchar(120) not null,
  status varchar(20) not null,
  message text,
  started_at timestamp not null default current_timestamp,
  finished_at timestamp,
  duration_ms bigint,
  created_at timestamp not null default current_timestamp
);

create index if not exists idx_scheduled_job_logs_job_created on scheduled_job_logs(job_id, created_at desc);

insert into scheduled_jobs (name, handler_name, cron_expression, parameters, enabled, description)
select
  'Read notification cleanup',
  'notification-cleanup',
  '0 0 3 * * ?',
  '{"retention_days":90}',
  true,
  'Delete read notifications older than the configured retention window.'
where not exists (
  select 1 from scheduled_jobs where handler_name = 'notification-cleanup'
);
