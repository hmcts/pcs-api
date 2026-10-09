ALTER TABLE public.document
  ADD COLUMN hearing_id INTEGER REFERENCES public.hearing (id);

CREATE INDEX idx_document_hearing_id ON public.document USING btree (hearing_id);
