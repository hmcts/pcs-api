package uk.gov.hmcts.reform.pcs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SuperUser {
    private String firstName;
    private String lastName;
    private String email;
}
