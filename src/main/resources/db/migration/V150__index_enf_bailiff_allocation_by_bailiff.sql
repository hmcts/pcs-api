-- Batch print (HDPI-8012) lists a bailiff's active allocations across every case. IDAM ids are compared
-- lower-cased, so the index is on the lower-cased id.
CREATE INDEX idx_enf_bailiff_allocation_active_bailiff
    ON public.enf_bailiff_allocation USING btree (lower(bailiff_user_id)) WHERE ended_at IS NULL;
