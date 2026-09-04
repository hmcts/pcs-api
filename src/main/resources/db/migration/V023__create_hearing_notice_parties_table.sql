-- Create table
CREATE TABLE public.hearing_notice_party
(
  hearing_id INTEGER NOT NULL REFERENCES public.hearing (id) ON DELETE CASCADE,
  party_id UUID NOT NULL REFERENCES public.party (id),
  CONSTRAINT hearing_notice_party_pkey PRIMARY KEY (hearing_id, party_id)
);

-- Migrate existing hearing notice party data
INSERT INTO public.hearing_notice_party (hearing_id, party_id)
SELECT
  h.id,
  p.party_id
FROM public.hearing h
       CROSS JOIN LATERAL unnest(h.notice_parties) AS p(party_id);
