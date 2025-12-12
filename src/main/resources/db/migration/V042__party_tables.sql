CREATE TYPE YES_NO AS ENUM ('YES', 'NO');

DROP TABLE public.claim_party;
DROP TABLE public.party;

CREATE TABLE public.party (
  id                       UUID PRIMARY KEY,
  version                  INTEGER,
  case_id                  UUID REFERENCES public.pcs_case (id),
  type                     TEXT,
  idam_id                  UUID,
  first_name               TEXT,
  last_name                TEXT,
  org_name                 TEXT,
  name_known               YES_NO,
  name_overridden          YES_NO,
  address_id               UUID REFERENCES public.address (id),
  address_known            YES_NO,
  address_same_as_property YES_NO,
  phone_number_provided    YES_NO,
  phone_number             TEXT,
  email_address            TEXT,
  pcq_id                   TEXT
);

CREATE INDEX idx_idam_id ON public.party (idam_id);

CREATE TABLE public.claim_party (
  claim_id UUID REFERENCES public.claim (id),
  party_id UUID REFERENCES public.party (id),
  role     TEXT NOT NULL,

  PRIMARY KEY (claim_id, party_id)
);
