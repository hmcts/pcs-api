CREATE TABLE flag_ref_data(
                            id UUID PRIMARY KEY,
                            flag_code VARCHAR(10),
                            name VARCHAR(255),
                            name_cy VARCHAR(255),
                            hearing_relevant BOOLEAN,
                            available_externally BOOLEAN,
                            visibility VARCHAR(20)
);

CREATE TABLE case_party_flag (
                               id UUID PRIMARY KEY,
                               party_id UUID REFERENCES party(id) ON DELETE CASCADE,
                               flag_ref_data_id UUID NOT NULL REFERENCES flag_ref_data(id) ON DELETE CASCADE,
                               sub_type_key VARCHAR(50),
                               sub_type_value VARCHAR(50),
                               sub_type_value_cy VARCHAR(50),
                               other_description VARCHAR(50),
                               other_description_cy VARCHAR(50),
                               flag_comment VARCHAR(255),
                               flag_comment_cy VARCHAR(255),
                               flag_update_comment VARCHAR(255),
                               flag_update_comment_cy VARCHAR(255),
                               date_time_created TIMESTAMP,
                               date_time_modified TIMESTAMP,
                               status VARCHAR(50) NOT NULL,
                               paths VARCHAR(255)
);

CREATE TABLE case_flag (
                            id UUID PRIMARY KEY,
                            pcs_case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
                            flag_ref_data_id UUID NOT NULL REFERENCES flag_ref_data(id) ON DELETE CASCADE,
                            sub_type_key VARCHAR(50),
                            sub_type_value VARCHAR(50),
                            sub_type_value_cy VARCHAR(50),
                            other_description VARCHAR(50),
                            other_description_cy VARCHAR(50),
                            flag_comment VARCHAR(255),
                            flag_comment_cy VARCHAR(255),
                            flag_update_comment VARCHAR(255),
                            flag_update_comment_cy VARCHAR(255),
                            date_time_created TIMESTAMP,
                            date_time_modified TIMESTAMP,
                            status VARCHAR(50) NOT NULL,
                            paths VARCHAR(255)
);
