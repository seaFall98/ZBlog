alter table comments add column if not exists user_id bigint;
alter table comments add column if not exists root_id bigint;

create index if not exists idx_comments_target_created
  on comments(target_type, target_key, created_at, id);

create index if not exists idx_comments_parent
  on comments(parent_id);

create index if not exists idx_comments_user
  on comments(user_id);
