package uk.gov.hmcts.reform.pcs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents the User held within the Organisation Response
 */
@AllArgsConstructor
@Data
public class SuperUser {
    private String firstName;
    private String lastName;
    private String email;
}
