-- Optimistic locking for the enforcement record. It is updated from the case event, the Payments callback and
-- the document generation task, which take no common lock, so a write from a stale copy has to be refused
-- rather than silently overwrite the other write.
ALTER TABLE public.enf_case
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
