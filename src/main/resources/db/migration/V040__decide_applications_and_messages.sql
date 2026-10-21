-- A judge decides a general application with an order on it, and court staff and judges message each other on a case.

-- An order deciding an application names it, and a judge can have a draft for each application beside their draft
-- for the case.
ALTER TABLE orders ADD COLUMN gen_app_id UUID REFERENCES general_application(id);

DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id, gen_app_id)
    NULLS NOT DISTINCT WHERE state = 'DRAFT';

-- Messages between court staff and judges on a case: a thread with a subject, which a sender starts and either side
-- replies to until someone closes it. A thread is open until it has a closed_at; when anyone wrote is on its entries.
CREATE TABLE case_message (
    id              UUID PRIMARY KEY,
    case_id         UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    subject         VARCHAR(60) NOT NULL,
    closed_at       TIMESTAMP WITH TIME ZONE,
    closed_by_name  TEXT,
    closure_note    VARCHAR(250),
    CHECK (closed_at IS NOT NULL OR (closed_by_name IS NULL AND closure_note IS NULL))
);

CREATE INDEX case_message_case_idx ON case_message(case_id);

-- What a message relates to: one or more categories.
CREATE TABLE case_message_category (
    message_id      UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    category        TEXT NOT NULL CHECK (category IN ('APPLICATION', 'HEARING', 'DOCUMENTS', 'COUNTERCLAIM',
                                                      'ENFORCEMENT_ACTION', 'OTHER')),
    PRIMARY KEY (message_id, category)
);

-- The application a message is about, if it is about one.
CREATE TABLE case_message_gen_app (
    message_id      UUID PRIMARY KEY REFERENCES case_message(id) ON DELETE CASCADE,
    gen_app_id      UUID NOT NULL REFERENCES general_application(id) ON DELETE CASCADE
);

CREATE INDEX case_message_gen_app_gen_app_idx ON case_message_gen_app(gen_app_id);

-- What each sender wrote, in order: the first message, then the replies.
CREATE TABLE case_message_entry (
    id              UUID PRIMARY KEY,
    message_id      UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    sender_name     TEXT NOT NULL,
    sender_role     TEXT NOT NULL,
    recipient_role  TEXT NOT NULL,
    urgent          BOOLEAN NOT NULL,
    body            TEXT NOT NULL
);

CREATE INDEX case_message_entry_message_idx ON case_message_entry(message_id, created_at);

-- The message an order step carries: the judge's message to court staff when they submit it, or court staff's query
-- when they return it, in place of a case note.
ALTER TABLE order_steps DROP COLUMN note_id;
ALTER TABLE order_steps ADD COLUMN message_entry_id UUID REFERENCES case_message_entry(id);
CREATE INDEX order_steps_message_entry_idx ON order_steps(message_entry_id) WHERE message_entry_id IS NOT NULL;
