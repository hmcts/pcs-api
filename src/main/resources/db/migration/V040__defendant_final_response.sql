-- Add defendant_final_response_submitted to pcs_case table to track submission of defendant response
ALTER TABLE public.pcs_case ADD COLUMN defendant_final_response_submitted boolean

