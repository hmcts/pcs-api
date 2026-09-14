-- HDPI-8866 W05: optimistic version on the respond-to-claim draft so the statement of truth can be
-- bound to the exact draft the citizen reviewed. Bumped by JPA (@Version) on every draft save.
ALTER TABLE draft.draft_case_data
  ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
