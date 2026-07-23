-- HDPI-7878: additional postcodes/courts for nightly e2e, avoiding the stale global_search index orphans.

INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES
    ('M1 1AE', 701411, 'England', CURRENT_DATE, NULL, '{"created_by": "admin", "change_reason": "HDPI-7878"}'::jsonb),
    ('B1 1AA', 231596, 'England', CURRENT_DATE, NULL, '{"created_by": "admin", "change_reason": "HDPI-7878"}'::jsonb),
    ('CF10 1EP', 234850, 'Wales', CURRENT_DATE, NULL, '{"created_by": "admin", "change_reason": "HDPI-7878"}'::jsonb)
ON CONFLICT (postcode, epims_id) DO NOTHING;

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit)
VALUES
    (701411, CURRENT_DATE, '{"created_by": "admin", "change_reason": "HDPI-7878"}'::jsonb),
    (231596, CURRENT_DATE, '{"created_by": "admin", "change_reason": "HDPI-7878"}'::jsonb),
    (234850, CURRENT_DATE, '{"created_by": "admin", "change_reason": "HDPI-7878"}'::jsonb)
ON CONFLICT (epims_id) DO NOTHING;
