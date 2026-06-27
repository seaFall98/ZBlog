alter table comments add column if not exists like_count bigint not null default 0;
alter table comments add column if not exists pinned boolean not null default false;
alter table comments add column if not exists pinned_at timestamp;
alter table comments add column if not exists pinned_by bigint;

create index if not exists idx_comments_target_status_pinned
  on comments(target_type, target_key, status, pinned, pinned_at, created_at);

create table if not exists comment_likes (
  comment_id bigint not null,
  user_id bigint not null,
  created_at timestamp not null default current_timestamp,
  primary key (comment_id, user_id),
  constraint fk_comment_likes_comment foreign key (comment_id) references comments(id) on delete cascade,
  constraint fk_comment_likes_user foreign key (user_id) references users(id) on delete cascade
);

create index if not exists idx_comment_likes_user on comment_likes(user_id);

alter table files add column if not exists storage_provider varchar(40);
alter table files add column if not exists storage_bucket varchar(255);
alter table files add column if not exists storage_region varchar(120);
alter table files add column if not exists storage_object_key varchar(1000);
alter table files add column if not exists storage_domain varchar(500);
alter table files add column if not exists storage_prefix varchar(500);
alter table files add column if not exists checksum_sha256 varchar(64);
alter table files add column if not exists uploaded_by bigint;
alter table files add column if not exists bound_comment_id bigint;
alter table files add column if not exists bound_at timestamp;

create index if not exists idx_files_upload_user_type_time
  on files(uploaded_by, upload_type, upload_time);

create index if not exists idx_files_bound_comment on files(bound_comment_id);
create index if not exists idx_files_checksum on files(checksum_sha256);

create table if not exists stats_daily (
  stat_date date primary key,
  pv bigint not null default 0,
  uv bigint not null default 0,
  article_view_count bigint not null default 0,
  event_count bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table if not exists stats_article_daily (
  stat_date date not null,
  article_id bigint not null,
  pv bigint not null default 0,
  uv bigint not null default 0,
  view_count_delta bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  primary key (stat_date, article_id),
  constraint fk_stats_article_daily_article foreign key (article_id) references articles(id) on delete cascade
);

create index if not exists idx_stats_article_daily_article_date
  on stats_article_daily(article_id, stat_date desc);

insert into scheduled_jobs (name, handler_name, cron_expression, parameters, enabled, description)
select 'Daily visit archive', 'daily-visit-archive', '0 10 1 * * ?', '{}', true,
       'Archive yesterday site and article visit statistics.'
where not exists (select 1 from scheduled_jobs where handler_name = 'daily-visit-archive');

insert into scheduled_jobs (name, handler_name, cron_expression, parameters, enabled, description)
select 'Article view count flush', 'article-view-flush', '0 * * * * ?', '{}', true,
       'Flush pending Redis article view-count deltas to the database.'
where not exists (select 1 from scheduled_jobs where handler_name = 'article-view-flush');

insert into scheduled_jobs (name, handler_name, cron_expression, parameters, enabled, description)
select 'SEO feed cache refresh', 'seo-feed-refresh', '0 30 2 * * ?', '{}', true,
       'Refresh RSS, Atom, and Sitemap XML cache.'
where not exists (select 1 from scheduled_jobs where handler_name = 'seo-feed-refresh');

insert into scheduled_jobs (name, handler_name, cron_expression, parameters, enabled, description)
select 'Scheduled article publish', 'article-scheduled-publish', '0 * * * * ?', '{}', true,
       'Publish due scheduled articles and run normal publish side effects.'
where not exists (select 1 from scheduled_jobs where handler_name = 'article-scheduled-publish');
