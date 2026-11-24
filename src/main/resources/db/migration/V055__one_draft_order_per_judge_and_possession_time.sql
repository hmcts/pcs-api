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
