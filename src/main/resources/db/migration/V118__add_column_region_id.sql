ALTER TABLE pcs_case ADD COLUMN region_id INTEGER;
ALTER TABLE pcs_case RENAME COLUMN case_management_location TO base_location
