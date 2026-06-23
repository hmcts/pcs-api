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
public class AntisocialAndConductTabDetails {

    @CCD(label = "Is there actual or threatened antisocial behaviour?")
    private String antiSocialBehaviour;

    @CCD(label = "Details of actual or threatened antisocial behaviour")
    private String antiSocialBehaviourDetails;

    @CCD(label = "Is there actual or threatened use of the premises for illegal purposes?")
    private String propertyUsedIllegally;

    @CCD(label = "Details of actual or threatened use of the premises for illegal purposes")
    private String propertyUsedIllegallyDetails;

    @CCD(label = "Has there been other prohibited conduct?")
    private String otherProhibitedConduct;

    @CCD(label = "Details of other prohibited conduct")
    private String otherProhibitedConductDetails;
}
