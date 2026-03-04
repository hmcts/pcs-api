CREATE SCHEMA IF NOT EXISTS ccd;

DROP TABLE IF EXISTS ccd.task_outbox;
DROP TYPE IF EXISTS ccd.task_action;

CREATE TYPE ccd.task_action AS ENUM ('cancel', 'complete', 'initiate', 'reconfigure');

CREATE TABLE ccd.task_outbox (
    id bigserial primary key,
    case_id text not null,
    case_type_id text not null,
    payload jsonb not null,
    action ccd.task_action not null,
    status text not null default 'NEW',
    attempt_count integer not null default 0,
    created timestamp not null default now(),
    updated timestamp not null default now(),
    processed timestamp,
    next_attempt_at timestamp,
    last_response_code integer,
    last_error text
);

CREATE INDEX idx_task_outbox_status_created ON ccd.task_outbox(status, created);
CREATE INDEX idx_task_outbox_status_next_attempt ON ccd.task_outbox(status, next_attempt_at);
