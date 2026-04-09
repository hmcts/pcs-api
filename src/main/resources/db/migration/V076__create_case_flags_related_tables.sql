CREATE TABLE flags (
                     id UUID NOT NULL PRIMARY KEY,
                     case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
                     party_id UUID NOT NULL REFERENCES party(id) ON DELETE CASCADE,
                     party_name VARCHAR(50),
                     role_on_case VARCHAR(50),
                     group_id VARCHAR(255),
                     visibility VARCHAR(50)

);


CREATE TABLE flag_details (
                            id UUID PRIMARY KEY,
                            flags_id UUID NOT NULL REFERENCES flags(id) ON DELETE CASCADE,
                            flag_code VARCHAR(6) NOT NULL,
                            name VARCHAR(50) NOT NULL,
                            name_cy VARCHAR(50) NOT NULL,
                            sub_type_value VARCHAR(50),
                            sub_type_value_cy VARCHAR(50),
                            sub_type_key VARCHAR(50),
                            other_description VARCHAR(50),
                            hearing_relevant BOOLEAN NOT NULL,
                            flag_comment VARCHAR(100),
                            flag_comment_cy VARCHAR(100),
                            flag_update_comment VARCHAR(100),
                            date_time_created TIMESTAMP NOT NULL,
                            date_time_modified TIMESTAMP,
                            path VARCHAR(100) NOT NULL,
                            status VARCHAR(50) NOT NULL,
                            available_externally BOOLEAN
);
