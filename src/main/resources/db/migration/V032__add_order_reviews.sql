-- A caseworker's review of an order a judge submitted: either a query returning it to the judge, or
-- the order as they issued it and how it is to be issued. An order returned to the judge can be
-- reviewed again once resubmitted, so an order can have several reviews, told apart by the version
-- of the order each reviewed: the judge is shown the query of the latest. The order as issued is the
-- judge's make order form and order document with any changes the caseworker made; the judge's order
-- in draft_orders stays as they submitted it. Audited in ccd.audit_log like draft_orders.
ALTER TABLE draft_orders DROP CONSTRAINT draft_orders_state_check;
ALTER TABLE draft_orders ADD CONSTRAINT draft_orders_state_check
    CHECK (state IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'RETURNED_TO_JUDGE', 'ISSUED'));

CREATE TABLE order_reviews (
    id UUID PRIMARY KEY,
    draft_order_id UUID NOT NULL REFERENCES draft_orders(id) ON DELETE CASCADE,
    reviewer_idam_user_id UUID NOT NULL,
    reviewed_version BIGINT NOT NULL,
    outcome TEXT NOT NULL CHECK (outcome IN ('RETURNED_TO_JUDGE', 'ISSUED')),
    query_to_judge TEXT,
    order_type TEXT,
    form_data JSONB,
    docweave_snapshot JSONB,
    next_steps_complete BOOLEAN,
    final_order BOOLEAN,
    serve_all_parties BOOLEAN,
    parties_served JSONB,
    seal TEXT CHECK (seal IN ('COUNTY_COURT', 'HIGH_COURT'))
);

CREATE INDEX order_reviews_draft_order_id_idx ON order_reviews(draft_order_id);

CALL ccd.attach_case_event_auditing_v1('public.order_reviews'::regclass);
