INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) VALUES
    ('CF11 1BB', 28837, 'Wales', '2025-01-01', NULL, '{"created_by": "admin", "change_reason": "wales test data"}'::jsonb)
ON CONFLICT (postcode, epims_id) DO NOTHING; 


