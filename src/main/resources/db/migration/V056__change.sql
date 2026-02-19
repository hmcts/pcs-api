-- ============================================
-- DROP OLD CASE LINK STRUCTURE
-- ============================================

-- Drop index on case_link if it exists
DROP INDEX IF EXISTS ux_case_link_unique;

-- Drop index on case_link_reason if it exists
DROP INDEX IF EXISTS idx_case_link_reason_link;

-- Drop case_link_reason table first (child table)
DROP TABLE IF EXISTS case_link_reason CASCADE;

-- Drop case_link table
DROP TABLE IF EXISTS case_link CASCADE;

-- ============================================
-- PCS CASE LINK + LINK REASONS
-- ============================================
-- ============================================
-- PCS CASE LINK + LINK REASONS
-- ============================================

-- Table: case_link
CREATE TABLE case_link (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Source case
                         case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,

  -- Linked case number (BIGINT)
                         linked_case_id BIGINT NOT NULL,

  -- CCD ListValue ID
                         ccd_list_id VARCHAR(50),

                         created_at TIMESTAMP DEFAULT now()
);

-- Unique constraint on source + linked case
CREATE UNIQUE INDEX uk_case_link_unique
  ON case_link(case_id, linked_case_id);

-- Table: case_link_reason
CREATE TABLE case_link_reason (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- FK to case_link
                                case_link_id UUID NOT NULL REFERENCES case_link(id) ON DELETE CASCADE,

                                reason_code VARCHAR(100) NOT NULL,
                                reason_text VARCHAR(255)
);

-- Optional index for performance
CREATE INDEX idx_case_link_reason_link
  ON case_link_reason(case_link_id);
