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

-- The tasks a judge has worked on an order from, which sending it for review completes: the one draft can be opened
-- from several.
CREATE TABLE order_task (
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    task_id  TEXT NOT NULL,
    PRIMARY KEY (order_id, task_id)
);
