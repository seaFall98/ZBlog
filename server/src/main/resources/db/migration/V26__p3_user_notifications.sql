alter table notifications add column if not exists recipient_user_id bigint;
alter table notifications add column if not exists target_type varchar(80);
alter table notifications add column if not exists target_key varchar(180);
alter table notifications add column if not exists target_comment_id bigint;

create index if not exists idx_notifications_recipient_read_created
  on notifications(recipient_user_id, is_read, created_at);

create index if not exists idx_notifications_target_comment
  on notifications(target_type, target_key, target_comment_id);
