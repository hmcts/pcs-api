ALTER TABLE public.document
  ADD COLUMN hearing_id INTEGER REFERENCES public.hearing (id);

ALTER TABLE public.hearing
  ADD COLUMN notice_number INTEGER,
  ADD CONSTRAINT uq_hearing_case_notice_number UNIQUE (case_id, notice_number);


CREATE INDEX idx_document_hearing_id ON public.document USING btree (hearing_id);
