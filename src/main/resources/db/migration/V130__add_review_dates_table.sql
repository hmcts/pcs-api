CREATE TABLE public.review_date
(
  id UUID PRIMARY KEY,
  case_id UUID REFERENCES pcs_case (id),
  date DATE NOT NULL,
  reason VARCHAR(60) NOT NULL,
  description VARCHAR(500) NOT NULL
);

CREATE INDEX idx_review_date_case_id ON public.review_date(case_id);
