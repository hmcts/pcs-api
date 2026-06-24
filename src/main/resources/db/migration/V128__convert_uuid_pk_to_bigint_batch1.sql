-- HDPI-7336: Convert internal-only UUID primary keys to BIGINT IDENTITY (batch 1).
--
-- Pre-go-live cutover migration: case data is intentionally cleared down (TRUNCATE)
-- and primary keys are re-typed. There is NO backfill - this is only valid before
-- go-live while no live cases exist. The application entities use
-- GenerationType.IDENTITY so the database now owns id generation.
--
-- Batch 1 tables (internal-only, no CCD ListValue exposure, no inbound-FK
-- complications beyond address):
--   address, rent_arrears, notice_of_possession, possession_alternatives,
--   asb_prohibited_conduct, tenancy_licence.

-- ---------------------------------------------------------------------------
-- address  (referenced by pcs_case.property_address_id, party.address_id,
--           legal_representative.address_id)
-- ---------------------------------------------------------------------------
TRUNCATE TABLE address CASCADE;

-- DROP COLUMN ... CASCADE also drops the inbound FK constraints referencing
-- address.id. The referencing columns themselves remain and are re-typed below.
ALTER TABLE address DROP COLUMN id CASCADE;
ALTER TABLE address ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

ALTER TABLE pcs_case             ALTER COLUMN property_address_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE party                ALTER COLUMN address_id          TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE legal_representative ALTER COLUMN address_id          TYPE BIGINT USING NULL::BIGINT;

ALTER TABLE pcs_case
    ADD CONSTRAINT fk_pcs_case_property_address
    FOREIGN KEY (property_address_id) REFERENCES address (id);
ALTER TABLE party
    ADD CONSTRAINT fk_party_address
    FOREIGN KEY (address_id) REFERENCES address (id);
ALTER TABLE legal_representative
    ADD CONSTRAINT fk_legal_representative_address
    FOREIGN KEY (address_id) REFERENCES address (id);

-- ---------------------------------------------------------------------------
-- Leaf claim child tables (no inbound foreign keys reference their id)
-- ---------------------------------------------------------------------------
TRUNCATE TABLE rent_arrears CASCADE;
ALTER TABLE rent_arrears DROP COLUMN id CASCADE;
ALTER TABLE rent_arrears ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

TRUNCATE TABLE notice_of_possession CASCADE;
ALTER TABLE notice_of_possession DROP COLUMN id CASCADE;
ALTER TABLE notice_of_possession ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

TRUNCATE TABLE possession_alternatives CASCADE;
ALTER TABLE possession_alternatives DROP COLUMN id CASCADE;
ALTER TABLE possession_alternatives ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

TRUNCATE TABLE asb_prohibited_conduct CASCADE;
ALTER TABLE asb_prohibited_conduct DROP COLUMN id CASCADE;
ALTER TABLE asb_prohibited_conduct ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

TRUNCATE TABLE tenancy_licence CASCADE;
ALTER TABLE tenancy_licence DROP COLUMN id CASCADE;
ALTER TABLE tenancy_licence ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
