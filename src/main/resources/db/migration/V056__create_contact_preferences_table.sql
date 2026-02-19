CREATE TABLE public.contact_preferences (
  id                  UUID PRIMARY KEY,
  contact_by_email    BOOLEAN DEFAULT NULL,
  contact_by_text     BOOLEAN DEFAULT NULL,
  contact_by_post     BOOLEAN DEFAULT NULL,
  contact_by_phone    BOOLEAN DEFAULT NULL
);