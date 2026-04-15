-- Add content_type and size columns to document table per HDPI-3928 AC07
ALTER TABLE document
    ADD COLUMN content_type VARCHAR(255),
    ADD COLUMN size BIGINT;

COMMENT ON COLUMN document.content_type IS 'MIME type of the document (e.g., application/pdf, image/jpeg)';
COMMENT ON COLUMN document.size IS 'File size in bytes';
