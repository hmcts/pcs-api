-- hearing_facts refers to a judicial note and to the judge who recorded it by id, which needs keys on
-- judicial_note and user_name.
ALTER TABLE judicial_note ADD PRIMARY KEY (id);
ALTER TABLE user_name ADD PRIMARY KEY (id);

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
