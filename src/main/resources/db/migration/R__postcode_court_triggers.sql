/*
  Trigger and function to clean up postcode values to have no spaces and be all in upper case
*/
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
  writing the preview environment uses Postgres 11, (even though Azure DBs
  for pcs-api are on 16).

  When the preview environment is using Postgres 14+ then the DROP/CREATE can be
  changed to a CREATE OR REPLACE
*/
DROP TRIGGER IF EXISTS postcode_court_mapping_trigger ON postcode_court_mapping;

CREATE TRIGGER postcode_court_mapping_trigger
  BEFORE INSERT OR UPDATE ON postcode_court_mapping
  FOR EACH ROW EXECUTE FUNCTION postcode_court_mapping_trigger_func();
