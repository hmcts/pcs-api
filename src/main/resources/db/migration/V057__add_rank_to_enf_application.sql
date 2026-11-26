-- Number each enforcement application per type on its case (writ 1, writ 2, warrant 1...), used in its document names
ALTER TABLE public.enf_application
ADD COLUMN rank integer;
