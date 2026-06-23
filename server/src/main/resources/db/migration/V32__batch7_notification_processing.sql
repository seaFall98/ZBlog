alter table notifications add column if not exists is_processed boolean not null default false;
alter table notifications add column if not exists processed_at timestamp;

create index if not exists idx_notifications_ops_filters
  on notifications (type, is_read, is_processed, created_at);
