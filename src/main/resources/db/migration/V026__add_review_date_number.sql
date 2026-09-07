ALTER TABLE public.case_review_date
  ADD COLUMN review_date_number INTEGER;

UPDATE public.case_review_date review_date
SET review_date_number = ranked_review_date.review_date_number
FROM (
  SELECT
    id,
    ROW_NUMBER() OVER (
      PARTITION BY case_id
      ORDER BY created_date ASC NULLS LAST, id ASC
    ) AS review_date_number
  FROM public.case_review_date
) ranked_review_date
WHERE review_date.id = ranked_review_date.id;

ALTER TABLE public.case_review_date
  ALTER COLUMN review_date_number SET NOT NULL;

ALTER TABLE public.case_review_date
  ADD CONSTRAINT uq_case_review_date_case_review_date_number UNIQUE (case_id, review_date_number);
