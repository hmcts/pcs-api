ALTER TABLE public.case_review_date
  ADD COLUMN created_by VARCHAR(255),
  ADD COLUMN created_on TIMESTAMP WITH TIME ZONE;
