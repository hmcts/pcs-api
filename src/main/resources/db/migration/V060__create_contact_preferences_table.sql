CREATE TABLE public.contact_preferences (
  id                  UUID PRIMARY KEY,
  contact_by_email    YES_NO,
  contact_by_text     YES_NO,
  contact_by_post     YES_NO,
  contact_by_phone    YES_NO
);

ALTER TABLE public.party
  ADD COLUMN contact_preferences_id UUID REFERENCES public.contact_preferences (id);