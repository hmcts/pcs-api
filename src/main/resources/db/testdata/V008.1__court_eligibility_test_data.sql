-- HDPI-1256: Test data for eligibility_whitelisted_epim table
-- This data contains sample whitelisted EPIMS IDs for testing court eligibility functionality
-- All entries are set to be eligible from 14th July 2025

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (20262, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb),   -- Central London County Court
    (28837, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb),   -- Test court entry
    (144641, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb),  -- Test court entry
    (425094, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb);  -- Test court entry 