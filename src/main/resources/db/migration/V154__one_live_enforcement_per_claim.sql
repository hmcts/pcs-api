-- A claim can have at most one live enforcement (EnforcementStatus.isLive), which until now only the
-- application code enforced. The not-live set is a fixed list of names, so a partial unique index expresses
-- the rule. Rows with no status are left out of the index, as EnforcementStatus.notLive() queries and
-- CurrentEnforcementService also ignore them.

-- Rows that pre-date the status model can already break the rule: V037 backfilled every application that
-- existed to AWAITING_PAYMENT and V053 moved the restitution ones to AWAITING_REVIEW, both live statuses, so
-- a claim that accrued more than one application before the status model has more than one live row. Close
-- all but the one CurrentEnforcementService already treats as the current enforcement -- the live row whose
-- status changed most recently, ordered as CurrentEnforcementService.CURRENT_FIRST orders it -- because that
-- is the only one any enforcement event or view acts on.
WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY claim_id
               ORDER BY status_updated_at DESC NULLS LAST, id
           ) AS live_rank
    FROM public.enf_case
    WHERE status IS NOT NULL
      AND status NOT IN ('CLOSED', 'REJECTED', 'TRANSFERRED_TO_HIGH_COURT')
)
UPDATE public.enf_case
SET status = 'CLOSED',
    status_updated_at = CURRENT_TIMESTAMP
WHERE id IN (SELECT id FROM ranked WHERE live_rank > 1);

CREATE UNIQUE INDEX enf_case_one_live_per_claim
    ON public.enf_case (claim_id)
    WHERE status IS NOT NULL
      AND status NOT IN ('CLOSED', 'REJECTED', 'TRANSFERRED_TO_HIGH_COURT');
