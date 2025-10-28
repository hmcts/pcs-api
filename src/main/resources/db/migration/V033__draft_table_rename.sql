-- Rename the table from unsubmitted_case_data to draft_case_data
ALTER TABLE draft.unsubmitted_case_data RENAME TO draft_case_data;

-- Rename the existing index to match the new table name
ALTER INDEX draft.unsubmitted_case_data_case_reference_idx RENAME TO draft_case_data_case_reference_idx;
