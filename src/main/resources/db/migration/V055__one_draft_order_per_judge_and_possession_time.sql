-- A judge has one draft order on a case, wherever it is made. The judge chooses the hearing a draft is made at, or
-- none, and can change it, so the hearing no longer tells one draft from another.

DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id)
    WHERE state = 'DRAFT';

-- The time on the day possession is ordered by, which an order may give (by default 4pm) or leave out. Possession
-- ordered forthwith has no day, so no time.
ALTER TABLE order_claim ADD COLUMN possession_time TIME;

ALTER TABLE order_claim ADD CONSTRAINT order_claim_possession_time_check
    CHECK (possession_time IS NULL OR possession_date IS NOT NULL);

-- An issued order either disposes of an application, closing it, or lists it for a hearing, leaving it open; the
-- order's wording says what was decided. An order that decided an application some other way left it open, as one
-- that does not deal with it does, so it has no row.
DROP INDEX order_gen_app_decided_once_idx;
ALTER TABLE order_gen_app DROP CONSTRAINT order_gen_app_decision_check;
DELETE FROM order_gen_app WHERE decision = 'OTHER';
UPDATE order_gen_app SET decision = CASE decision WHEN 'LIST' THEN 'LISTED' ELSE 'DISPOSED' END;
ALTER TABLE order_gen_app ADD CONSTRAINT order_gen_app_decision_check CHECK (decision IN ('DISPOSED', 'LISTED'));
CREATE UNIQUE INDEX order_gen_app_decided_once_idx ON order_gen_app(gen_app_id) WHERE decision = 'DISPOSED';
