-- HDPI-1256: Update eligibility_whitelisted_epim test data to use current dates
-- This updates the hardcoded dates to current date for eligibility and current timestamp for audit

UPDATE eligibility_whitelisted_epim 
SET 
    eligible_from = CURRENT_DATE,
    audit = jsonb_build_object('generated, R1, V1', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))
WHERE epims_id IN (20262, 28837, 144641, 425094); 