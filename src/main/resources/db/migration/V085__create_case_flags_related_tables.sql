CREATE TABLE flag_details (
                            id UUID PRIMARY KEY,
                            case_id UUID REFERENCES pcs_case(id) ON DELETE CASCADE,
                            party_id UUID REFERENCES party(id) ON DELETE CASCADE,
                            flag_code VARCHAR(6) NOT NULL,
                            sub_type_key VARCHAR(50),
                            sub_type_value VARCHAR(50),
                            sub_type_value_cy VARCHAR(50),
                            other_description VARCHAR(50),
                            other_description_cy VARCHAR(50),
                            flag_comment VARCHAR(255),
                            flag_comment_cy VARCHAR(255),
                            flag_update_comment VARCHAR(255),
                            date_time_created TIMESTAMP,
                            date_time_modified TIMESTAMP,
                            status VARCHAR(50) NOT NULL
);

CREATE TABLE flag_path(
                        id UUID PRIMARY KEY,
                        flag_details_id UUID NOT NULL REFERENCES flag_details (id) ON DELETE CASCADE,
                        path VARCHAR(255) NOT NULL
                      );

CREATE TABLE ref_data_flags(
                             id UUID PRIMARY KEY,
                             flag_code VARCHAR(10),
                             name VARCHAR(255),
                             name_cy VARCHAR(255),
                             hearing_relevant BOOLEAN,
                             available_externally BOOLEAN,
                             visibility VARCHAR(20)
);
