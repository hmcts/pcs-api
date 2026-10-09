-- Orders replace draft_orders, whose drafts are not kept. An order lives on one orders row, which holds
-- where it is and, once issued, its seal and document. Each change to the order appends an order_steps
-- row with the order as it then stood; the latest step is the current order, and its id is the order's
-- version. A step may point at a case_note, such as the query a caseworker returns an order with.
DROP TABLE draft_orders;

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    author_idam_user_id UUID NOT NULL,
    -- The judge's name, for the order's document.
    author_name TEXT,
    state TEXT NOT NULL CHECK (state IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'RETURNED_TO_JUDGE', 'ISSUED')),
    seal TEXT CHECK (seal IN ('COUNTY_COURT', 'HIGH_COURT')),
    document_id UUID REFERENCES document(id)
);

CREATE INDEX orders_case_id_idx ON orders(case_id);
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id)
    WHERE state = 'DRAFT';

CALL ccd.attach_case_event_auditing_v1('public.orders'::regclass);

CREATE TABLE order_steps (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    state TEXT NOT NULL CHECK (state IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'RETURNED_TO_JUDGE', 'ISSUED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_idam_user_id UUID NOT NULL,
    order_type TEXT NOT NULL,
    form_data JSONB NOT NULL,
    docweave_snapshot JSONB,
    order_html TEXT,
    note_id INTEGER REFERENCES case_note(id),
    -- The issuing step, and only it, carries the wording as issued; the step that returns an order to its
    -- judge carries the query, and the judge's saves while it is returned carry none.
    CONSTRAINT order_steps_html_check CHECK ((state = 'ISSUED') = (order_html IS NOT NULL)),
    CONSTRAINT order_steps_note_check CHECK (note_id IS NULL OR state = 'RETURNED_TO_JUDGE')
);

CREATE INDEX order_steps_order_id_idx ON order_steps(order_id);
-- An order is issued once.
CREATE UNIQUE INDEX order_steps_one_issue_idx ON order_steps(order_id) WHERE state = 'ISSUED';
