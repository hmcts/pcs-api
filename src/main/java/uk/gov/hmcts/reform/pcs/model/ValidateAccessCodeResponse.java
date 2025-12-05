package uk.gov.hmcts.reform.pcs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAccessCodeResponse {

    private long caseReference;
    private String status;

}
