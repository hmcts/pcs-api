UPDATE postcode_court_mapping
SET postcode = upper(regexp_replace(postcode, '\s', '', 'g'));
