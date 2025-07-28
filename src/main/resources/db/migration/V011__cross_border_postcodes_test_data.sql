-- Insert cross-border postcodes test data
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) VALUES
-- England entries
('SY132LH', 20262, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('TD151', 144641, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('CH14Q', 20262, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('CH14QJ', 20262, 'England', '2025-07-14', '2025-07-14', '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('TD90TU', 99990, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('TD151UU', 212121, 'England', '2025-07-14', '2025-07-13', '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('LL520NU', 88888, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('LD37HP', 88888, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),

-- Wales entries
('SY132LH', 28837, 'Wales', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('CH14Q', 99999, 'Wales', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('CH14QJ', 99999, 'Wales', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('LL52', 28837, 'Wales', '2025-07-14', '2025-07-14', '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),

-- Scotland entries
('TD151', 425094, 'Scotland', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('TD90TU', 10101, 'Scotland', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('TD151UU', 313131, 'Scotland', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'); 