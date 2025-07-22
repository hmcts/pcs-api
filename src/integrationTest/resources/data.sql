INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit)
VALUES ('W3 7RX', 20262, 'England', '2025-01-01', '2035-04-01', '{}'::jsonb),
       ('W3 6RS', 36791, 'England', '2025-02-01', NULL, '{}'::jsonb),
       ('W3 6RT', 36791, 'England', '2025-02-01', '2025-07-01', '{}'::jsonb),
       ('RH13', 20262, 'England', '2025-01-01', '2035-04-01', '{}'::jsonb),
       ('RH13', 36791, 'England', '2025-02-01', NULL, '{}'::jsonb),
       ('M13 9PL', 144641, 'England', '2025-04-01', '2035-04-01', '{}'::jsonb),
       ('TST1 9BC', 1000, 'England', '2025-01-01', NULL, '{}'::jsonb),
       ('tst2 9BC', 1000, 'England', '2025-01-01', '2025-12-31', '{}'::jsonb),
       (' TsT3    9 B C ', 1000, 'England', '2025-01-01', NULL, '{}'::jsonb),
       ('SY132LH', 20262, 'England', '2025-01-01', NULL, '{}'::jsonb),
       ('SY132LH', 28837, 'Wales', '2025-01-01', NULL, '{}'::jsonb),
       ('TD151', 144641, 'England', '2025-01-01', NULL, '{}'::jsonb),
       ('TD151', 425094, 'Scotland', '2025-01-01', NULL, '{}'::jsonb),
       ('CH14Q', 20262, 'England', '2025-01-01', NULL, '{}'::jsonb),
       ('CH14Q', 99999, 'Wales', '2025-01-01', NULL, '{}'::jsonb),
       ('CH14QJ', 20262, 'England', '2025-01-01', '2025-07-14', '{}'::jsonb),
       ('CH14QJ', 99999, 'Wales', '2025-01-01', NULL, '{}'::jsonb),
       ('TD90TU', 99990, 'England', '2025-01-01', NULL, '{}'::jsonb),
       ('TD90TU', 10101, 'Scotland', '2025-01-01', NULL, '{}'::jsonb),
       ('TD151UU', 212121, 'England', '2025-01-01', '2025-07-13', '{}'::jsonb),
       ('TD151UU', 313131, 'Scotland', '2025-01-01', NULL, '{}'::jsonb),
       ('LL520NU', 28837, 'Wales', '2025-01-01', NULL, '{}'::jsonb),
       ('LD37HP', 88888, 'Wales', '2025-01-01', NULL, '{}'::jsonb),
       ('LL52', 28837, 'Wales', '2025-01-01', '2025-07-14', '{}'::jsonb);


INSERT INTO eligibility_whitelisted_epim (epims_id, eligible_from, audit) VALUES
    (20262, '2025-07-14', '{}'::jsonb),
    (36791, '2025-07-14', '{}'::jsonb),
    (28837, '2025-07-14', '{}'::jsonb),
    (425094, '2025-07-14', '{}'::jsonb);
