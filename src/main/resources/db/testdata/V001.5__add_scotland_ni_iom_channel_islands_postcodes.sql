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


