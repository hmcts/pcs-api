-- A judge's draft of an order on a case, from first draft until it is issued. Issued orders are a
-- separate record. Every write happens in a CCD case event and is audited in ccd.audit_log, which
-- records when, by whom and through which event, so the table keeps no timestamps.
CREATE TABLE draft_orders (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    author_idam_user_id UUID NOT NULL,
    state TEXT NOT NULL CHECK (state IN ('DRAFT', 'SUBMITTED_FOR_REVIEW')),
    version BIGINT NOT NULL DEFAULT 0,
    order_type TEXT NOT NULL,
    form_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    docweave_snapshot JSONB
);

CREATE INDEX draft_orders_case_id_idx ON draft_orders(case_id);
CREATE UNIQUE INDEX draft_orders_one_draft_per_author_idx ON draft_orders(case_id, author_idam_user_id)
    WHERE state = 'DRAFT';

-- TODO: add an optional hearing association when order-to-hearing rules are defined.
CALL ccd.attach_case_event_auditing_v1('public.draft_orders'::regclass);
