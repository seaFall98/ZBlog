alter table friends add column if not exists rss_status varchar(40) not null default 'pending';
alter table friends add column if not exists rss_last_fetch_at timestamp;
alter table friends add column if not exists rss_last_error text not null default '';

create unique index if not exists uq_rss_feed_articles_friend_link on rss_feed_articles (friend_id, link);
