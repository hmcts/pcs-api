CREATE TYPE order_state AS ENUM (
    'DRAFT',
    'SUBMITTED_FOR_REVIEW'
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id),
    state order_state NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    draft_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX orders_case_id_idx ON orders(case_id);
CREATE UNIQUE INDEX orders_one_draft_per_case_idx ON orders(case_id) WHERE state = 'DRAFT';

-- TODO: add an optional hearing association when order-to-hearing rules are defined.
CALL ccd.attach_case_event_auditing_v1('public.orders'::regclass);
