-- RFC 5321 allows 254 characters; citizen IDAM emails can exceed the old limits.
-- notice_details carries the address a notice was emailed to.
ALTER TABLE public.party ALTER COLUMN email_address TYPE varchar(254);
ALTER TABLE public.notice_of_possession ALTER COLUMN notice_details TYPE varchar(254);
