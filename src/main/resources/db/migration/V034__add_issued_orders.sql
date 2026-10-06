-- An order as the court issued it: the review that issued it, its wording as HTML exported from the
-- order document the caseworker issued, and the sealed document generated from that wording once
-- Docmosis has rendered it. An order is issued once, so a draft order has at most one issued order.
-- Unlike draft_orders it is not audited in ccd.audit_log, which allows writes only in a case event: a
-- background task attaches the document. The issuing event's review in order_reviews is audited.
CREATE TABLE issued_orders (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    draft_order_id UUID NOT NULL UNIQUE REFERENCES draft_orders(id) ON DELETE CASCADE,
    order_review_id UUID NOT NULL REFERENCES order_reviews(id) ON DELETE CASCADE,
    order_type TEXT NOT NULL,
    order_html TEXT NOT NULL,
    seal TEXT NOT NULL CHECK (seal IN ('COUNTY_COURT', 'HIGH_COURT')),
    final_order BOOLEAN NOT NULL,
    issued_by_idam_user_id UUID NOT NULL,
    issued_on DATE NOT NULL,
    document_id UUID REFERENCES document(id)
);

CREATE INDEX issued_orders_case_id_idx ON issued_orders(case_id);
