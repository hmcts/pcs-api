-- The "Other" flag's other_description carries the name of the category the party chose it under,
-- and several category names (and most of their Welsh translations) exceed 50 characters, e.g.
-- "I need something to feel comfortable during my hearing". Widen to match flag_comment.

ALTER TABLE case_flag
    ALTER COLUMN other_description TYPE varchar(255),
    ALTER COLUMN other_description_cy TYPE varchar(255);

ALTER TABLE case_party_flag
    ALTER COLUMN other_description TYPE varchar(255),
    ALTER COLUMN other_description_cy TYPE varchar(255);
