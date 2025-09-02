INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES ('CF11 6QX', 30100, 'Wales', '2025-08-29', NULL, '{"created_by": "admin", "change_reason": "initial insert"}');

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (30100, '2025-01-01', '{"generated, R1, V1": "2025-09-01T08:00:00.000Z"}'::jsonb);
