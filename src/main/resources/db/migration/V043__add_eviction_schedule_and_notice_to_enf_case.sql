-- The bailiff schedules the eviction (HDPI-7344) and marks the notice of eviction as served (HDPI-7346).
-- The eviction date itself is the existing bailiff_date column, which drives the claimant's Confirm eviction
-- journey. Times are UK wall-clock times, as bailiff_date is.
--
-- dispense_with_service: whether the court has given permission to dispense with service of the notice of
--   eviction. Null until the eviction is scheduled.
-- notice_served_at / notice_served_how: when and how the bailiff served the notice of eviction.
-- defendants_can_see_enforcement: derived on every change, true once the notice is served and false when
--   service is dispensed with. Kept on the row so that defendant visibility of the enforcement can be driven
--   from it later (HDPI-7539, HDPI-8442).
ALTER TABLE public.enf_case
    ADD COLUMN dispense_with_service BOOLEAN,
    ADD COLUMN notice_served_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN notice_served_how VARCHAR(500),
    ADD COLUMN defendants_can_see_enforcement BOOLEAN NOT NULL DEFAULT FALSE;
