package uk.gov.hmcts.reform.pcs.idam;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class User {
    private String authToken;
    private UserInfo userDetails;
}
