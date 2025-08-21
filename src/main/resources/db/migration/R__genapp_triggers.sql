/*
  Trigger and function to insert unique incrementing genapp reference
*/
CREATE OR REPLACE FUNCTION genapp_ordering_func()
  RETURNS trigger
  LANGUAGE plpgsql
AS $func$
DECLARE
  caseReference varchar;
  genappCount integer;
BEGIN
  SELECT case_reference INTO caseReference FROM pcs_case WHERE id = NEW.case_id;

  SELECT count(*) INTO genappCount FROM gen_app WHERE case_id = NEW.case_id;
  genappCount := genappCount + 1;

  NEW.gen_app_reference = concat_ws('-', caseReference, 'ga', genappCount);
  RETURN NEW;
END;
$func$;

CREATE OR REPLACE TRIGGER genapp_ordering_trigger
  BEFORE INSERT ON gen_app
  FOR EACH ROW EXECUTE FUNCTION genapp_ordering_func();
