-- A judge's order decides general applications and the claim, and court staff and judges message each other on a case.

-- A judge has one draft per hearing on a case, and one made without a hearing.
DROP INDEX orders_one_draft_per_author_idx;
CREATE UNIQUE INDEX orders_one_draft_per_author_idx ON orders(case_id, author_idam_user_id, hearing_id)
    NULLS NOT DISTINCT WHERE state = 'DRAFT';

-- An issued order's recipients are looked up from its document.
CREATE INDEX orders_document_id_idx ON orders(document_id) WHERE document_id IS NOT NULL;

-- The parties an order made without notice is served on; an order with none is served on all parties.
CREATE TABLE order_served_party (
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES party(id),
    PRIMARY KEY (order_id, party_id)
);

-- What an issued order decided about each application it covers. An application is granted, dismissed or struck out
-- by one order only; listing it for a hearing, or any other decision, leaves it to be decided.
CREATE TABLE order_gen_app (
    order_id   UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    gen_app_id UUID NOT NULL REFERENCES general_application(id),
    decision   TEXT NOT NULL CHECK (decision IN ('GRANT', 'DISMISS', 'STRIKE_OUT', 'LIST', 'OTHER')),
    PRIMARY KEY (order_id, gen_app_id)
);

CREATE UNIQUE INDEX order_gen_app_decided_once_idx ON order_gen_app(gen_app_id)
    WHERE decision IN ('GRANT', 'DISMISS', 'STRIKE_OUT');

-- What an issued order decides about a claim, recorded the same way however its wording was written. An order that
-- makes no order on the claim, such as an adjournment, which manages the case without deciding it, has no row.
CREATE TABLE order_claim (
    order_id               UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    claim_id               UUID NOT NULL REFERENCES claim(id),
    kind                   TEXT NOT NULL CHECK (kind IN ('OUTRIGHT_POSSESSION', 'SUSPENDED_POSSESSION', 'STRUCK_OUT',
                                                         'DISMISSED', 'OTHER')),
    possession_forthwith   BOOLEAN,
    possession_date        DATE,
    grounds                TEXT CHECK (grounds IN ('MANDATORY', 'DISCRETIONARY')),
    grounds_details        TEXT,
    warrant_on_notice      BOOLEAN,
    PRIMARY KEY (order_id, claim_id),
    -- Possession is ordered forthwith or by a date, and a suspended order says the date.
    CONSTRAINT order_claim_outright_check
        CHECK (kind <> 'OUTRIGHT_POSSESSION' OR (possession_forthwith IS TRUE) = (possession_date IS NULL)),
    CONSTRAINT order_claim_suspended_check CHECK (kind <> 'SUSPENDED_POSSESSION' OR possession_date IS NOT NULL)
);

CREATE INDEX order_claim_claim_id_idx ON order_claim(claim_id);

-- A message thread between court staff and judges on a case. Either side replies until someone closes it;
-- it is open while closed_at is null.
CREATE TABLE case_message (
    id                     UUID PRIMARY KEY,
    case_id                UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    subject                VARCHAR(60) NOT NULL,
    -- The application the thread is about, if it is about one.
    gen_app_id             UUID REFERENCES general_application(id),
    closed_at              TIMESTAMPTZ,
    closed_by_idam_user_id TEXT,
    closed_by_name         TEXT,
    closure_note           VARCHAR(250),
    -- Who closed a thread is recorded exactly when it is closed; only a closed thread has a note.
    CONSTRAINT case_message_closed_check CHECK (
        num_nulls(closed_at, closed_by_idam_user_id, closed_by_name) IN (0, 3)
        AND (closed_at IS NOT NULL OR closure_note IS NULL))
);

CREATE INDEX case_message_case_id_idx ON case_message(case_id);

-- What a thread relates to.
CREATE TABLE case_message_category (
    message_id UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    category   TEXT NOT NULL CHECK (category IN ('APPLICATION', 'HEARING', 'DOCUMENTS', 'COUNTERCLAIM',
                                                 'ENFORCEMENT_ACTION', 'OTHER')),
    PRIMARY KEY (message_id, category)
);

-- What was written in a thread, oldest first: the opening message, then the replies.
CREATE TABLE case_message_entry (
    id                  UUID PRIMARY KEY,
    message_id          UUID NOT NULL REFERENCES case_message(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL,
    sender_idam_user_id TEXT NOT NULL,
    sender_name         TEXT NOT NULL,
    sender_side         TEXT NOT NULL CHECK (sender_side IN ('COURT_STAFF', 'JUDGE')),
    -- The role whose pool is asked to review the entry.
    recipient_role      TEXT NOT NULL CHECK (recipient_role IN ('COURT_STAFF', 'DISTRICT_JUDGE', 'CIRCUIT_JUDGE',
                                                                'DEPUTY_DISTRICT_JUDGE')),
    urgent              BOOLEAN NOT NULL,
    body                TEXT NOT NULL
);

CREATE INDEX case_message_entry_message_idx ON case_message_entry(message_id, created_at);

-- The query court staff return an order with is an entry in a closed thread, on the step that returns it.
ALTER TABLE order_steps DROP COLUMN note_id,
    ADD COLUMN message_entry_id UUID REFERENCES case_message_entry(id),
    ADD CONSTRAINT order_steps_message_check CHECK (message_entry_id IS NULL OR state = 'RETURNED_TO_JUDGE');

-- What an issued order says about costs. Any sum to be paid is a money judgment.
ALTER TABLE orders ADD COLUMN costs TEXT CHECK (costs IN (
    'DEFENDANT_PAYS', 'DEFENDANT_PAYS_FIXED', 'DEFENDANT_PAYS_SUMMARY', 'CLAIMANT_PAYS_SUMMARY',
    'PUBLIC_FUNDING', 'COSTS_IN_CASE', 'RESERVED', 'NO_ORDER', 'OTHER'));

-- A sum an issued order says one side pays the other. Different sums or terms for different parties are separate
-- judgments.
CREATE TABLE money_judgment (
    id                    UUID PRIMARY KEY,
    order_id              UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    arrears               NUMERIC(18,2) CHECK (arrears >= 0),
    -- Interest and any other sum.
    other_amount          NUMERIC(18,2) CHECK (other_amount >= 0),
    costs                 NUMERIC(18,2) CHECK (costs >= 0),
    -- Paid by a one-off payment, by the whole or the balance by a date, by instalments, or any of these. With none,
    -- it is due within 14 days (CPR 40.11).
    one_off_amount        NUMERIC(18,2) CHECK (one_off_amount > 0),
    one_off_date          DATE,
    pay_by                DATE,
    instalment_amount     NUMERIC(18,2) CHECK (instalment_amount > 0),
    instalment_frequency  TEXT CHECK (instalment_frequency IN ('WEEKLY', 'FORTNIGHTLY', 'MONTHLY')),
    first_instalment_date DATE,
    -- Its enforcement is suspended on the terms the possession order is suspended on.
    suspended             BOOLEAN NOT NULL,
    status                TEXT NOT NULL CHECK (status IN ('ACTIVE', 'SATISFIED', 'CANCELLED', 'SET_ASIDE')),
    status_date           DATE,
    CONSTRAINT money_judgment_sum_check CHECK (num_nonnulls(arrears, other_amount, costs) > 0),
    CONSTRAINT money_judgment_one_off_check CHECK (num_nulls(one_off_amount, one_off_date) IN (0, 2)),
    CONSTRAINT money_judgment_instalments_check
        CHECK (num_nulls(instalment_amount, instalment_frequency, first_instalment_date) IN (0, 3)),
    CONSTRAINT money_judgment_status_date_check CHECK ((status = 'ACTIVE') = (status_date IS NULL))
);

CREATE INDEX money_judgment_order_id_idx ON money_judgment(order_id);

-- Who owes a money judgment and who is owed it. More than one debtor are jointly and severally liable.
CREATE TABLE money_judgment_party (
    judgment_id UUID NOT NULL REFERENCES money_judgment(id) ON DELETE CASCADE,
    party_id    UUID NOT NULL REFERENCES party(id),
    role        TEXT NOT NULL CHECK (role IN ('DEBTOR', 'CREDITOR')),
    PRIMARY KEY (judgment_id, party_id)
);
