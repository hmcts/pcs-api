-- XUI asks what order an applicant wants as rich text, and CCD does not limit a RichTextArea's length.

ALTER TABLE general_application
ALTER COLUMN what_order_wanted TYPE text;
