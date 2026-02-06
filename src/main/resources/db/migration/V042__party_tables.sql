CREATE TYPE YES_NO AS ENUM ('YES', 'NO');

DROP TABLE public.claim_party;
DROP TABLE public.party;

CREATE TABLE public.party (
  id                       UUID PRIMARY KEY,
  version                  INTEGER,
  case_id                  UUID REFERENCES public.pcs_case (id),
  type                     TEXT,
  idam_id                  UUID,
  first_name               VARCHAR(60),
  last_name                VARCHAR(60),
  org_name                 VARCHAR(60),
  name_known               YES_NO,
  name_overridden          YES_NO,
  address_id               UUID REFERENCES public.address (id),
  address_known            YES_NO,
  address_same_as_property YES_NO,
  phone_number_provided    YES_NO,
  phone_number             VARCHAR(60),
  email_address            VARCHAR(60),
  pcq_id                   VARCHAR(60)
);

CREATE INDEX idx_idam_id ON public.party (idam_id);

CREATE TABLE public.claim_party (
  claim_id    UUID REFERENCES public.claim (id),
  party_id    UUID REFERENCES public.party (id),
  role        TEXT NOT NULL,

  PRIMARY KEY (claim_id, party_id)
);

CREATE TABLE public.contact_preferences (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  party_id            UUID NOT NULL REFERENCES public.party (id),
  contact_by_email    BOOLEAN DEFAULT FALSE,
  contact_by_text     BOOLEAN DEFAULT FALSE,
  contact_by_post     BOOLEAN DEFAULT FALSE,
  contact_by_phone    BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_contact_preferences_party_id ON public.contact_preferences (party_id);

ALTER TABLE public.pcs_case DROP COLUMN defendant_details;
ALTER TABLE public.pcs_case DROP COLUMN underlessee_mortgagee_details;
