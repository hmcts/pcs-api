-- Create table
CREATE TABLE public.hearing_notice_party
(
  hearing_id INTEGER NOT NULL REFERENCES public.hearing (id) ON DELETE CASCADE,
  party_id UUID NOT NULL REFERENCES public.party (id),
  CONSTRAINT hearing_notice_party_pkey PRIMARY KEY (hearing_id, party_id)
);
