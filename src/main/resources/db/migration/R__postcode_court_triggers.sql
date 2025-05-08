CREATE OR REPLACE FUNCTION postcode_court_mapping_trigger_func()
  RETURNS trigger
  LANGUAGE plpgsql
AS $func$
BEGIN
  NEW.postcode = upper(regexp_replace(NEW.postcode, '\s', '', 'g'));
  RETURN NEW;
END;
$func$;

/*
  CREATE OR REPLACE TRIGGER was only added in Postgres 14 and at the time of
  writing, the Cftlib Postgres used in local development is still on
  version 12, (even though Azure DBs for the pcs-api are on 16).

  When Cftlib is using Postgres 14+ then the DROP/CREATE can be
  changed to a CREATE OR REPLACE
*/
DROP TRIGGER IF EXISTS postcode_court_mapping_trigger ON postcode_court_mapping;

CREATE TRIGGER postcode_court_mapping_trigger
  BEFORE INSERT OR UPDATE ON postcode_court_mapping
  FOR EACH ROW EXECUTE FUNCTION postcode_court_mapping_trigger_func();
