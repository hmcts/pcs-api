/*
  Trigger and function to insert unique incrementing claim reference
*/
CREATE OR REPLACE FUNCTION claim_ordering_func()
  RETURNS trigger
  LANGUAGE plpgsql
AS $func$
DECLARE
  caseReference varchar;
  postFix varchar;
  claimTypeCount integer;
BEGIN
  SELECT case_reference INTO caseReference FROM pcs_case WHERE id = NEW.case_id;

  postFix := CASE WHEN NEW.type = 'COUNTER_CLAIM' THEN 'cc'
                  WHEN NEW.type = 'MAIN_CLAIM' THEN 'mc'
    END;

  IF postFix IS NULL THEN
    RAISE EXCEPTION '%', 'Unknown claim type: ' || NEW.type;
  END IF;

  SELECT count(*) INTO claimTypeCount FROM claim WHERE case_id = NEW.case_id AND type = NEW.type;
  claimTypeCount := claimTypeCount + 1;

  NEW.claim_reference = concat_ws('-', caseReference, postFix, claimTypeCount);
  RETURN NEW;
END;
$func$;

CREATE OR REPLACE TRIGGER claim_ordering_trigger
  BEFORE INSERT ON claim
  FOR EACH ROW EXECUTE FUNCTION claim_ordering_func();
