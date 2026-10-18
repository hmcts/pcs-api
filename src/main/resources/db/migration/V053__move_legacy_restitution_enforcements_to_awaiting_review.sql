-- V037 put every enforcement that already existed in AWAITING_PAYMENT, including the restitution applications,
-- which have no fee. AWAITING_PAYMENT is a live status, so each of them blocks a new application on its claim,
-- and no Payments callback will ever move it on. Move them to AWAITING_REVIEW, where a fee-free application
-- starts (EnforcementStatusTransitions.initialStatus).
--
-- A row V037 backfilled is one still in AWAITING_PAYMENT with no status_updated_at: every status the service
-- sets goes through EnforcementStatusTransitions, which always records the time, so rows submitted after V037
-- are never matched. A row with a fee payment against it is left for the payment callback.
--
-- The warrant and writ of possession rows V037 backfilled stay in AWAITING_PAYMENT. They do owe a fee, but no
-- Payments service request was ever raised for them, so they stay blocked until a service request is raised
-- or the rows are closed by hand.
UPDATE public.enf_case
SET status = 'AWAITING_REVIEW',
    status_updated_at = CURRENT_TIMESTAMP
WHERE status = 'AWAITING_PAYMENT'
  AND status_updated_at IS NULL
  AND COALESCE(
        enforcement_order #>> '{chooseEnforcementType,value,code}',
        enforcement_order #>> '{chooseEnforcementType,valueCode}',
        enforcement_order ->> 'selectEnforcementType'
      ) IN ('WARRANT_OF_RESTITUTION', 'WRIT_OF_RESTITUTION')
  AND NOT EXISTS (
        SELECT 1 FROM public.fee_payment fp WHERE fp.related_entity_id = enf_case.id
      );
