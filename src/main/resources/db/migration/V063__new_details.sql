CREATE TABLE flags (
                     id UUID PRIMARY KEY,
                     party_name VARCHAR(255),
                     role_on_case VARCHAR(255),
                     visibility VARCHAR(50),
                     group_id UUID
);

CREATE TABLE flag_details (
                            id UUID PRIMARY KEY,
                            flags_id UUID,
                            name VARCHAR(255),
                            name_cy VARCHAR(255),
                            sub_type_value VARCHAR(255),
                            sub_type_value_cy VARCHAR(255),
                            sub_type_key VARCHAR(255),
                            other_description TEXT,
                            other_description_cy TEXT,
                            flag_comment TEXT,
                            flag_comment_cy TEXT,
                            flag_update_comment TEXT,
                            date_time_created TIMESTAMP,
                            date_time_modified TIMESTAMP,
                            hearing_relevant VARCHAR(10),
                            flag_code VARCHAR(100),
                            status VARCHAR(50),
                            available_externally VARCHAR(10),
                            CONSTRAINT fk_flags
                              FOREIGN KEY (flags_id)
                                REFERENCES flags(id)
);

CREATE TABLE flag_detail_path (
                                id UUID PRIMARY KEY,
                                flag_detail_id UUID,
                                value_id UUID,
                                path_value VARCHAR(255),
                                CONSTRAINT fk_flag_detail
                                  FOREIGN KEY (flag_detail_id)
                                    REFERENCES flag_details(id)
);
