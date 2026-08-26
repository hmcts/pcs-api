CREATE TYPE order_state AS ENUM (
    'DRAFT',
    'SUBMITTED_FOR_REVIEW',
    'ISSUED'
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id),
    hearing_id INTEGER REFERENCES hearing(id),
    state order_state NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    draft_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX orders_case_id_idx ON orders(case_id);
CREATE INDEX orders_hearing_id_idx ON orders(hearing_id);
CREATE UNIQUE INDEX orders_one_draft_per_hearing_idx ON orders(hearing_id) WHERE state = 'DRAFT';

CALL ccd.attach_case_event_auditing_v1('public.orders'::regclass);
