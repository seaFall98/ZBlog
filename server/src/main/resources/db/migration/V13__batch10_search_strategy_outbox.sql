create table if not exists search_index_status (
  id int primary key,
  strategy varchar(40) not null default 'db',
  elasticsearch_enabled boolean not null default false,
  fallback_to_db boolean not null default true,
  last_reindex_at timestamp,
  last_reindex_indexed int not null default 0,
  last_reindex_deleted int not null default 0,
  last_reindex_failed int not null default 0,
  last_error text,
  last_error_at timestamp,
  updated_at timestamp not null default current_timestamp
);

insert into search_index_status (id, strategy, elasticsearch_enabled, fallback_to_db)
select 1, 'db', false, true
where not exists (select 1 from search_index_status where id = 1);
