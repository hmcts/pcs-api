package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantContactTabDetails {

    @CCD (label = "Email address for notifications")
    private String emailAddress;

    @CCD (label = "Contact phone number")
    private String phoneNumber;
}
