CREATE TABLE public.wa_tasks (
   id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
   case_id UUID REFERENCES pcs_case (id),
   task_type TEXT NOT NULL,
   action TEXT NOT NULL,
   task_details jsonb,
   requested_at timestamp without time zone DEFAULT now() NOT NULL
);
