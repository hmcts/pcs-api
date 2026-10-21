-- XUI now asks what order an applicant wants as rich text, whose HTML tags are on top of the 6,800 characters
-- the applicant may write, and CCD does not limit a RichTextArea's length.

ALTER TABLE general_application
ALTER COLUMN what_order_wanted TYPE text;
