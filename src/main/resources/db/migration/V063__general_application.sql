CREATE TABLE general_application (
  id       UUID PRIMARY KEY,
  case_id  UUID REFERENCES public.pcs_case (id),
  type     VARCHAR(50),
  party_id UUID REFERENCES public.party (id)
)
