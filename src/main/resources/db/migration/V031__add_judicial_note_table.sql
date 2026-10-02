CREATE TABLE public.user_name (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  name varchar(50) NOT NULL,
  idam_id uuid NOT NULL
);

CREATE TABLE public.judicial_note (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  created_on timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  note varchar(30000) NOT NULL,
  user_id integer,
  case_id uuid
);
