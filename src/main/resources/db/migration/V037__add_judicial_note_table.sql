CREATE TABLE public.user_name (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text NOT NULL,
  idam_id text NOT NULL
);

CREATE UNIQUE INDEX ux_user_name_idam_id ON public.user_name (idam_id);

CREATE TABLE public.judicial_note (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_on timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  note varchar(30000) NOT NULL,
  user_id integer REFERENCES public.user_name(id),
  case_id uuid REFERENCES public.pcs_case(id)
);

CREATE INDEX judicial_note_case_id_idx ON public.judicial_note (case_id);

-- The hearing a judge made the order for; an order made without a hearing has none.
ALTER TABLE orders ADD COLUMN hearing_id INTEGER REFERENCES hearing(id);

-- What a judge recorded at a hearing: the case's facts as they stood then, which the next hearing starts
-- from, and the hearing's judicial note. Written whenever a judge saves the hearing's order.
CREATE TABLE hearing_facts (
    hearing_id INTEGER PRIMARY KEY REFERENCES hearing(id) ON DELETE CASCADE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id INTEGER NOT NULL REFERENCES user_name(id),
    updated_on TIMESTAMPTZ NOT NULL,
    judicial_note_id INTEGER UNIQUE REFERENCES judicial_note(id) ON DELETE SET NULL,
    tenancy_start_date DATE,
    tenancy_type TEXT,
    notice_date DATE,
    arrears_at_notice NUMERIC(18, 2),
    arrears_at_issue NUMERIC(18, 2),
    current_rent NUMERIC(18, 2),
    rent_frequency TEXT,
    grounds_pleaded TEXT,
    last_payment_date DATE,
    last_payment NUMERIC(18, 2),
    arrears_today NUMERIC(18, 2)
);

CALL ccd.attach_case_event_auditing_v1('public.hearing_facts'::regclass);
