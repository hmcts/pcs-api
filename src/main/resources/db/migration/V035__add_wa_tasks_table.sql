CREATE TABLE public.wa_tasks (
   id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
   case_id UUID REFERENCES pcs_case (id),
   task_type VARCHAR(60) NOT NULL,
   action VARCHAR(60) NOT NULL,
   task_details jsonb,
   requested_at timestamp without time zone DEFAULT now() NOT NULL
);
