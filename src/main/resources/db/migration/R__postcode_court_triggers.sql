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

CREATE OR REPLACE TRIGGER postcode_court_mapping_trigger
  BEFORE INSERT OR UPDATE ON postcode_court_mapping
  FOR EACH ROW EXECUTE FUNCTION postcode_court_mapping_trigger_func();
