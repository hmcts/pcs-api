-- Add Timestamps submission date to each of the existing tables !!!
ALTER TABLE public.enf_warrant ADD COLUMN submission_date DATE;
ALTER TABLE public.enf_writ ADD COLUMN submission_date DATE;
ALTER TABLE public.enf_warrant_of_restitution ADD COLUMN submission_date DATE;
ALTER TABLE public.enf_writ_of_restitution ADD COLUMN submission_date DATE;

-- Add Language used to each existing table
ALTER TABLE public.enf_warrant ADD COLUMN language_used VARCHAR(30);
ALTER TABLE public.enf_writ ADD COLUMN language_used VARCHAR(30);
ALTER TABLE public.enf_warrant_of_restitution ADD COLUMN language_used VARCHAR(30);
ALTER TABLE public.enf_writ_of_restitution ADD COLUMN language_used VARCHAR(30);

ALTER TABLE public.enf_warrant ADD COLUMN show_change_name_address_page YES_NO;

ALTER TABLE public.enf_writ ADD COLUMN was_general_application_to_transfer_to_high_court_successful YES_NO;
ALTER TABLE public.enf_writ ADD COLUMN completed_by STATEMENT_OF_TRUTH_COMPLETED_BY;
ALTER TABLE public.enf_writ ADD COLUMN certification TEXT;

  -- Repayment
  repayment_choice                        VARCHAR(20),
  amount_of_repayment_costs               NUMERIC(10, 2),


ALTER TABLE public.enf_risk_details ADD COLUMN enforcement_risk_categories VARCHAR(6800);

-- Not needed within Writ
ALTER TABLE public.enf_writ DROP COLUMN show_people_who_will_be_evicted_page;



