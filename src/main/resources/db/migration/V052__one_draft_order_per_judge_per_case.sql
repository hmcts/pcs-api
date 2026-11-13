-- A judge has one draft order on a case, wherever it is made. The judge chooses the hearing a draft is made at, or
-- none, and can change it, so the hearing no longer tells one draft from another.

-- Existing drafts are never merged or removed to fit: a judge with more than one on a case stops the migration.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM orders WHERE state = 'DRAFT'
               GROUP BY case_id, author_idam_user_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'A judge has more than one draft order on a case. Each judge must be left with one draft '
            'on each case before this migration runs; it does not merge or remove any';
    END IF;
END $$;

DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id)
    WHERE state = 'DRAFT';

-- A judge can discard their draft. It is kept, with every step it had, and is no longer their draft on the case.
ALTER TABLE orders DROP CONSTRAINT orders_state_check,
    ADD CONSTRAINT orders_state_check
        CHECK (state IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'RETURNED_TO_JUDGE', 'ISSUED', 'DISCARDED'));
ALTER TABLE order_steps DROP CONSTRAINT order_steps_state_check,
    ADD CONSTRAINT order_steps_state_check
        CHECK (state IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'RETURNED_TO_JUDGE', 'ISSUED', 'DISCARDED'));
