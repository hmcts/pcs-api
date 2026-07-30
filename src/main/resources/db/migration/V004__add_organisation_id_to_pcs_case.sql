-- The organisation that owns the case, captured from the creating user at case creation.
-- Drives the CCD CaseAccessGroups stamp used for professional group access, so it must be
-- intrinsic to the case rather than derived from whoever is reading it.
ALTER TABLE pcs_case ADD COLUMN organisation_id varchar(64);
