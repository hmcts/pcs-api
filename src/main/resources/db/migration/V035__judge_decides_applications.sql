-- A judge decides a general application by making an order on it. Court staff refer the application to a
-- judge, and the order the judge makes is for that application, which issuing it records the outcome of.

-- The application a judge's order was started from. A judge can have a draft for each application
-- alongside their draft for the case.
ALTER TABLE draft_orders ADD COLUMN gen_app_id UUID REFERENCES general_application(id) ON DELETE CASCADE;

DROP INDEX draft_orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX draft_orders_one_draft_per_author_idx ON draft_orders(case_id, author_idam_user_id, gen_app_id)
    NULLS NOT DISTINCT WHERE state = 'DRAFT';

-- The latest referral of the application to a judge, and the outcome of the order that decided it.
ALTER TABLE general_application
    ADD COLUMN referred_on DATE,
    ADD COLUMN referral_note TEXT,
    ADD COLUMN outcome TEXT CHECK (outcome IN ('GRANTED', 'REFUSED', 'STRUCK_OUT', 'LISTED', 'OTHER')),
    ADD COLUMN decided_by_order_id UUID REFERENCES issued_orders(id);
