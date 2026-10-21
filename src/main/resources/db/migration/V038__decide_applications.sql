-- A judge decides a general application with an order on it: the judge's order names it, and issuing the
-- order records what it decided on the application.

-- A judge can have a draft for each application beside their draft for the case.
ALTER TABLE orders ADD COLUMN gen_app_id UUID REFERENCES general_application(id);

DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id, gen_app_id)
    NULLS NOT DISTINCT WHERE state = 'DRAFT';

-- What the order deciding the application decided.
ALTER TABLE general_application
    ADD COLUMN outcome TEXT CHECK (outcome IN ('GRANTED', 'DISMISSED', 'STRUCK_OUT', 'LISTED', 'OTHER'));
