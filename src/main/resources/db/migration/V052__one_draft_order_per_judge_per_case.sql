-- A judge has one draft order on a case, wherever it is made. The judge chooses the hearing a draft is made at, or
-- none, and can change it, so the hearing no longer tells one draft from another.

DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id)
    WHERE state = 'DRAFT';
