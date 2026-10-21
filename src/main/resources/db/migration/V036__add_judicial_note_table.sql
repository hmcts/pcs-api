CREATE TABLE public.user_name (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  name varchar(50) NOT NULL,
  idam_id uuid NOT NULL
);

CREATE UNIQUE INDEX ux_user_name_idam_id ON public.user_name (idam_id);

CREATE TABLE public.judicial_note (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  created_on timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  note varchar(30000) NOT NULL,
  user_id integer,
  case_id uuid
);

CREATE UNIQUE INDEX ux_judicial_note_case_id ON public.judicial_note (case_id);

ALTER TABLE ONLY public.judicial_note
  ADD CONSTRAINT judicial_note_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);

ALTER TABLE ONLY public.judicial_note
  ADD CONSTRAINT judicial_note_user_name_fkey FOREIGN KEY (user_id) REFERENCES public.user_name(id);
