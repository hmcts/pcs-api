-- flag_ref_data holds one row per flag code, shared by every case using that flag. Nothing stopped
-- two concurrent writers inserting the same code, and once duplicated, the lookup by flag code fails
-- for every later save of that flag - for caseworkers as well as for parties.

-- Repoint any flags referencing a duplicate at the surviving row for their code. The duplicates
-- cannot simply be deleted first: case_flag and case_party_flag cascade on delete of their
-- reference data, which would take real case flags with them.
WITH duplicate AS (
    SELECT id,
           first_value(id) OVER (PARTITION BY flag_code ORDER BY id) AS keep_id
    FROM flag_ref_data
    WHERE flag_code IS NOT NULL
)
UPDATE case_flag
SET flag_ref_data_id = duplicate.keep_id
FROM duplicate
WHERE case_flag.flag_ref_data_id = duplicate.id
  AND duplicate.id <> duplicate.keep_id;

WITH duplicate AS (
    SELECT id,
           first_value(id) OVER (PARTITION BY flag_code ORDER BY id) AS keep_id
    FROM flag_ref_data
    WHERE flag_code IS NOT NULL
)
UPDATE case_party_flag
SET flag_ref_data_id = duplicate.keep_id
FROM duplicate
WHERE case_party_flag.flag_ref_data_id = duplicate.id
  AND duplicate.id <> duplicate.keep_id;

WITH duplicate AS (
    SELECT id,
           first_value(id) OVER (PARTITION BY flag_code ORDER BY id) AS keep_id
    FROM flag_ref_data
    WHERE flag_code IS NOT NULL
)
DELETE FROM flag_ref_data
USING duplicate
WHERE flag_ref_data.id = duplicate.id
  AND duplicate.id <> duplicate.keep_id;

ALTER TABLE flag_ref_data
  ADD CONSTRAINT flag_ref_data_flag_code_key UNIQUE (flag_code);
