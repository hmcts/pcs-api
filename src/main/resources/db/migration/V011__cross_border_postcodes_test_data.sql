-- Insert cross-border postcodes test data
-- Only include entries that don't overlap with src/integrationTest/resources/data.sql
INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, effective_from, effective_to, audit) 
VALUES
-- England entries
('TD90TU', 99990, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('LL520NU', 88888, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),
('LD37HP', 88888, 'England', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),

-- Wales entry
('LL52', 28837, 'Wales', '2025-07-14', '2025-07-14', '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'),

-- Scotland entry
('TD90TU', 10101, 'Scotland', '2025-07-14', NULL, '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}');

-- Update existing entries from src/integrationTest/resources/data.sql with new effective dates for cross-border testing
-- This ensures the existing entries are properly set up for cross-border testing
UPDATE postcode_court_mapping 
SET effective_from = '2025-07-14', 
    audit = '{"created_by": "system", "created_date": "2025-01-01T00:00:00Z"}'
WHERE (postcode, legislative_country) IN (
    ('SY132LH', 'England'),
    ('SY132LH', 'Wales'),
    ('TD151', 'England'),
    ('TD151', 'Scotland'),
    ('CH14Q', 'England'),
    ('CH14Q', 'Wales'),
    ('CH14QJ', 'England'),
    ('CH14QJ', 'Wales'),
    ('TD151UU', 'England'),
    ('TD151UU', 'Scotland')
); 