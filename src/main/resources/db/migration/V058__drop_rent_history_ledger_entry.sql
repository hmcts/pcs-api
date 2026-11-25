-- The ledger rent history design was dropped in favour of the table, so nothing stores ledger entries.
-- rent_arrears.proof_type is now UPLOAD_STATEMENT or TABLE.
DROP TABLE IF EXISTS public.rent_history_ledger_entry;
