ALTER TABLE public.party
  ADD COLUMN contact_preferences_id UUID REFERENCES public.contact_preferences (id);

UPDATE public.party p
  SET contact_preferences_id = cp.id
  FROM public.contact_preferences cp
  WHERE cp.party_id = p.id;