-- A judge decides a general application with an order on it, and court staff and judges message each other on a case.

-- A judge has a draft for each hearing beside their draft made without one; an application has no draft of its own.
DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id, hearing_id)
    NULLS NOT DISTINCT WHERE state = 'DRAFT';

-- The parties court staff served an order on without notice; an order with none is served on all parties.
CREATE TABLE order_served_party (
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES party(id),
    PRIMARY KEY (order_id, party_id)
);

-- Messages between court staff and judges on a case: a thread with a subject, which a sender starts and either side
-- replies to until someone closes it. A thread is open until it has a closed_at; when anyone wrote is on its entries.
CREATE TABLE case_message (
    id              UUID PRIMARY KEY,
    case_id         UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    subject         VARCHAR(60) NOT NULL,
    closed_at       TIMESTAMP WITH TIME ZONE,
    closed_by_idam_user_id TEXT,
    closed_by_name  TEXT,
    closure_note    VARCHAR(250),
    -- The application a message is about, if it is about one.
    gen_app_id      UUID REFERENCES general_application(id) ON DELETE SET NULL,
    -- Who closed it is known exactly when it is closed; a note is only ever with a closed message.
    CHECK ((closed_at IS NULL) = (closed_by_idam_user_id IS NULL)),
    CHECK ((closed_at IS NULL) = (closed_by_name IS NULL)),
    CHECK (closed_at IS NOT NULL OR closure_note IS NULL)
);

CREATE INDEX case_message_case_idx ON case_message(case_id);
CREATE INDEX case_message_gen_app_idx ON case_message(gen_app_id) WHERE gen_app_id IS NOT NULL;

-- What a message relates to: one or more categories.
CREATE TABLE case_message_category (
    message_id      UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    category        TEXT NOT NULL CHECK (category IN ('APPLICATION', 'HEARING', 'DOCUMENTS', 'COUNTERCLAIM',
                                                      'ENFORCEMENT_ACTION', 'OTHER')),
    PRIMARY KEY (message_id, category)
);

-- What each sender wrote, in order: the first message, then the replies.
CREATE TABLE case_message_entry (
    id              UUID PRIMARY KEY,
    message_id      UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    sender_idam_user_id TEXT NOT NULL,
    sender_name     TEXT NOT NULL,
    -- The sender's side; the recipient's role routes the message to that role's pool.
    sender_side     TEXT NOT NULL CHECK (sender_side IN ('COURT_STAFF', 'JUDGE')),
    recipient_role  TEXT NOT NULL CHECK (recipient_role IN ('COURT_STAFF', 'DISTRICT_JUDGE', 'CIRCUIT_JUDGE',
                                                            'DEPUTY_DISTRICT_JUDGE')),
    urgent          BOOLEAN NOT NULL,
    body            TEXT NOT NULL
);

CREATE INDEX case_message_entry_message_idx ON case_message_entry(message_id, created_at);

-- Court staff's query when they return an order, kept as a closed message in place of a case note.
ALTER TABLE order_steps ADD COLUMN message_entry_id UUID REFERENCES case_message_entry(id),
    ADD CONSTRAINT order_steps_message_check CHECK (message_entry_id IS NULL OR state = 'RETURNED_TO_JUDGE');
CREATE INDEX order_steps_message_entry_idx ON order_steps(message_entry_id) WHERE message_entry_id IS NOT NULL;

-- Existing queries keep their note_id.
