create table outbox_event (
    event_id uuid primary key,
    customer_id uuid not null,
    payload text not null,
    created_at timestamptz not null default now(),
    published_at timestamptz
);

create index outbox_event_pending_idx
    on outbox_event (created_at) where published_at is null;