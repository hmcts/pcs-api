-- HDPI-7834: consolidated test data (merged from the former V001.1-V001.9)
-- Order preserved - later sections update rows inserted by earlier ones.

-- ===== from V001.1__postcode_test_data.sql =====
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) VALUES
    ('W3 7RX', 20262, 'England', '2025-01-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('W3 6RS', 36791, 'England', '2025-02-01', NULL, '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('M13 9PL', 144641, 'England', '2025-04-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('LE2 0QB', 425094, 'England', '2023-04-01', '2024-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('UB7 0DG', 28837, 'England', '2026-04-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('SW4 7R', 36791, 'England', '2025-05-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('W5 7', 144641, 'England', '2025-05-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('UB2', 20262, 'England', '2025-05-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('SW1 1AA', 817113, 'England', '2025-06-16', NULL, '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('SW1 1AA', 817114, 'England', '2025-06-01', NULL, '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('W95', 20263, 'England', '2025-06-01', '2025-06-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('W95', 20262, 'England', '2025-06-11', '2035-07-31', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('DN5 51P', 999985, 'England', '2025-06-01', '2035-11-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('DN5 51P', 999984, 'England', '2025-01-01', NULL, '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('SL6', 144641, 'England', '2025-06-01', '2035-11-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('M13 9PZ', 817114, 'England', '2025-01-01', '2025-02-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('M13 9P', 484482, 'England', '2025-03-01', '2025-04-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('M13 9', 20262, 'England', '2025-06-01', '2035-07-31', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('M13', 144641, 'England', '2025-06-01', NULL, '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('RG6 1JS', 814567, 'England', '2025-03-01', '2025-04-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb);



-- ===== from V001.2__court_eligibility_test_data.sql =====
INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (20262, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb),
    (28837, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb),
    (144641, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb),
    (425094, '2025-07-14', '{"generated, R1, V1": "2025-07-08T10:02:39.968Z"}'::jsonb);

-- ===== from V001.3__update_eligibility_dates_to_current.sql =====
-- HDPI-1256: Update eligibility_whitelisted_epim test data to use current dates
-- This updates the hardcoded dates to current date for eligibility and current timestamp for audit

UPDATE eligibility_whitelisted_epim 
SET 
    eligible_from = CURRENT_DATE,
    audit = jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))
WHERE epims_id IN (20262, 28837, 144641, 425094); 
-- ===== from V001.4__update_postcode_test_data_with_crossborder_postcodes.sql =====
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) VALUES
    ('SY132LH', 20262, 'England', '2025-06-01', NULL, '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb),
    ('SY132LH', 28837, 'Wales', '2025-03-01', '2035-04-10', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb);



-- ===== from V001.5__add_scotland_ni_iom_channel_islands_postcodes.sql =====
-- Add postcode mappings for Scotland, Northern Ireland, Isle of Man, and Channel Islands
-- HDPI-1269: Handling Scotland, Northern Ireland, Channel Islands & IoM postcodes

INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES
    -- Northern Ireland postcodes
    ('BT8', 99991, 'Northern Ireland', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),
    ('BT93', 99992, 'Northern Ireland', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),

    -- Channel Islands postcodes
    ('JE3', 99993, 'Channel Islands', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),
    ('GY10', 99994, 'Channel Islands', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),

    -- Isle of Man postcodes
    ('IM1', 99995, 'Isle of Man', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),

    -- Scotland postcodes (additional to existing TD1)
    ('AB10', 99996, 'Scotland', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),

    -- Cross-border postcode TD1 (already exists but ensuring consistency)
    ('TD1', 99997, 'Scotland', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))),
    ('TD1', 99998, 'England', CURRENT_DATE, NULL, jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')));



-- ===== from V001.6__add_postcodes_for_local_test_address.sql =====
/* Insert the postcode used by the fake address lookup when running locally with CftLib */
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) VALUES
    ('SW111PD', 20264, 'England', '2025-01-01', '2035-04-01', '{"created_by": "admin", "change_reason": "initial insert"}'::jsonb);

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (20264, '2025-01-01', '{"generated, R1, V1": "2025-09-01T08:00:00.000Z"}'::jsonb);

-- ===== from V001.7__add_wales_postcodes_and_courts.sql =====
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES ('CF116QX', 30100, 'Wales', '2025-08-29', NULL, '{"created_by": "admin", "change_reason": "initial insert"}');

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (30100, '2025-01-01', '{"generated, R1, V1": "2025-09-01T08:00:00.000Z"}'::jsonb);

-- ===== from V001.8__add_england_wales_nocourt_postcodes.sql =====
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES ('SY45NN', 11111, 'England', '2025-08-29', NULL, '{"created_by": "admin", "change_reason": "initial insert"}');
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES ('SY45NN', 22222, 'Wales', '2025-08-29', NULL, '{"created_by": "admin", "change_reason": "initial insert"}');

-- ===== from V001.9__update_epim_test_data.sql =====
UPDATE postcode_court_mapping SET epims_id = 20262 WHERE postcode = 'CF116QX';

-- ===== from V132__repoint_cf116qx_court_mapping.sql (was a MIGRATION on master; its
-- reference-data change must survive the baseline consolidation on preview/local) =====
UPDATE postcode_court_mapping
SET epims_id = 366572
WHERE postcode = 'CF116QX';

INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit)
VALUES (366572, '2025-01-01', '{"created_by": "admin", "change_reason": "repoint CF116QX"}'::jsonb)
ON CONFLICT (epims_id) DO NOTHING;
