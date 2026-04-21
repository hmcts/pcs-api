ALTER TABLE public.document
ADD COLUMN enf_case_id uuid REFERENCES enf_case(id);
CREATE INDEX idx_document_enf_case_id ON document(enf_case_id);

-- Statement of Truth
ALTER TABLE enf_case
ADD COLUMN sot_id uuid REFERENCES statement_of_truth(id);

ALTER TABLE public.enf_warrant_of_restitution

-- How the defendants returned to the property
ADD COLUMN how_defendants_returned                  VARCHAR(6800),

-- PropertyAccessDetails
ADD COLUMN is_difficult_to_access_property          YES_NO,
ADD COLUMN clarification_on_access_difficulty_text  VARCHAR(6800),

-- Additional Information
ADD COLUMN additional_information_select            YES_NO,
ADD COLUMN additional_information_details           VARCHAR(6800);