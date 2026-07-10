UPDATE postcode_court_mapping
SET epims_id = 366572
WHERE postcode = 'CF116QX';

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit)
VALUES (366572, '2025-01-01', '{"created_by": "admin", "change_reason": "repoint CF116QX"}'::jsonb)
ON CONFLICT (epims_id) DO NOTHING;
