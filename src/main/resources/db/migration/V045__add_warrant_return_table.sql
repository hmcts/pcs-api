CREATE TABLE public.warrant_return (
  id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  enf_warrant_id INTEGER NOT NULL,
  return_type TEXT NOT NULL,
  outcome TEXT,
  return_name TEXT NOT NULL,
  additional_details TEXT,
  outcome_date TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP NOT NULL
);
