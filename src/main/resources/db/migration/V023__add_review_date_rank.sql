ALTER TABLE public.case_review_date
  ADD COLUMN rank INTEGER;

UPDATE public.case_review_date review_date
SET rank = ranked_review_date.rank
FROM (
  SELECT
    id,
    ROW_NUMBER() OVER (
      PARTITION BY case_id
      ORDER BY created_date ASC NULLS LAST, id ASC
    ) AS rank
  FROM public.case_review_date
) ranked_review_date
WHERE review_date.id = ranked_review_date.id;

ALTER TABLE public.case_review_date
  ALTER COLUMN rank SET NOT NULL;

ALTER TABLE public.case_review_date
  ADD CONSTRAINT uq_case_review_date_case_rank UNIQUE (case_id, rank);
