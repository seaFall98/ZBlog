alter table mail_outbox add column if not exists attempts int not null default 0;
alter table mail_outbox add column if not exists last_attempt_at timestamp;
alter table mail_outbox add column if not exists next_attempt_at timestamp;
alter table mail_outbox add column if not exists updated_at timestamp not null default current_timestamp;

create index if not exists idx_mail_outbox_status_next_attempt on mail_outbox (status, next_attempt_at);
