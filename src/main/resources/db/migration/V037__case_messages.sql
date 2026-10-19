-- Messages between court staff and judges on a case: a thread with a subject, which a sender starts and either side
-- replies to until someone closes it. A message can be about an application, and a judge's order can carry one.
CREATE TABLE case_message (
    id              UUID PRIMARY KEY,
    case_id         UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    subject         VARCHAR(60) NOT NULL,
    categories      TEXT NOT NULL,
    gen_app_id      UUID REFERENCES general_application(id) ON DELETE SET NULL,
    order_id        UUID REFERENCES orders(id) ON DELETE SET NULL,
    status          TEXT NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_by_name  TEXT,
    closure_note    VARCHAR(250)
);

CREATE INDEX case_message_case_idx ON case_message(case_id);

-- What each sender wrote, in order: the first message, then the replies.
CREATE TABLE case_message_entry (
    id              UUID PRIMARY KEY,
    message_id      UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    sender_name     TEXT NOT NULL,
    sender_role     TEXT NOT NULL,
    recipient_role  TEXT NOT NULL,
    urgent          BOOLEAN NOT NULL,
    body            VARCHAR(750) NOT NULL
);

CREATE INDEX case_message_entry_message_idx ON case_message_entry(message_id);
