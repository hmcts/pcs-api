-- The time on the day possession is ordered by, which an order may give (by default 4pm) or leave out. Possession
-- ordered forthwith has no day, so no time.
ALTER TABLE order_claim ADD COLUMN possession_time TIME;

ALTER TABLE order_claim ADD CONSTRAINT order_claim_possession_time_check
    CHECK (possession_time IS NULL OR possession_date IS NOT NULL);
