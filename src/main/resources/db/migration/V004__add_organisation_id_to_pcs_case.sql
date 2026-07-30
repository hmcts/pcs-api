-- Organisation that owns the case, used for professional group access.
ALTER TABLE pcs_case ADD COLUMN organisation_id varchar(64);
