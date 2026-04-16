CREATE TABLE flag_details (
                            id UUID PRIMARY KEY,
                            case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
                            flag_code VARCHAR(6) NOT NULL,
                            name VARCHAR(255) NOT NULL,
                            name_cy VARCHAR(255) NOT NULL,
                            sub_type_key VARCHAR(50),
                            sub_type_value VARCHAR(50),
                            sub_type_value_cy VARCHAR(50),
                            other_description VARCHAR(50),
                            other_description_cy VARCHAR(50),
                            hearing_relevant BOOLEAN NOT NULL,
                            flag_comment VARCHAR(255),
                            flag_comment_cy VARCHAR(255),
                            flag_update_comment VARCHAR(255),
                            date_time_created TIMESTAMP,
                            date_time_modified TIMESTAMP,
                            path VARCHAR(255),
                            status VARCHAR(50) NOT NULL,
                            available_externally BOOLEAN
);

CREATE TABLE flag_path(
                        id UUID PRIMARY KEY,
                        flag_details_id UUID NOT NULL REFERENCES flag_details (id) ON DELETE CASCADE,
                        path VARCHAR(255) NOT NULL
                      );

