ALTER TABLE public.party
  ADD COLUMN contact_address_id UUID,
  ADD COLUMN contact_email VARCHAR(100),
  ADD COLUMN contact_phone_number VARCHAR(20),
  ADD CONSTRAINT fk_contact_address
    FOREIGN KEY (contact_address_id)
      REFERENCES public.address(id);
