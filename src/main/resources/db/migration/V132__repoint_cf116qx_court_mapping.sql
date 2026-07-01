-- CF11 6QX (Cardiff) was mapped to the wrong court (EPIMS 234850); repoint it to 366572.
-- Idempotent: a no-op where the mapping is already correct (e.g. pr-1984), and a fix
-- where the DB still holds the stale 234850 row (local/other envs).

DELETE FROM postcode_court_mapping
WHERE postcode = 'CF116QX' AND epims_id = 234850;

INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES ('CF116QX', 366572, 'Wales', '2025-08-29', NULL,
        '{"created_by": "admin", "change_reason": "repoint CF116QX to court EPIMS 366572"}')
ON CONFLICT (postcode, epims_id) DO NOTHING;
