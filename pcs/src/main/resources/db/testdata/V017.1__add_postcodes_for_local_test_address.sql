/* Insert the postcode used by the fake address lookup when running locally with CftLib */
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) VALUES
    ('SW111PD', 20264, 'England', '2025-01-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb);

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (20264, '2025-01-01', '{"generated, R1, V1": "2025-09-01T08:00:00.000Z"}'::jsonb);
