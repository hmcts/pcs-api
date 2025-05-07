ALTER TABLE public.postcode_court_mapping
ALTER COLUMN effective_from TYPE DATE,
ALTER COLUMN effective_from SET NOT NULL,
ALTER COLUMN effective_to TYPE DATE;
