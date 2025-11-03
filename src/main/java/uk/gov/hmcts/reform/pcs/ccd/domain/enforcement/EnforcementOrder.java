package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * The main domain model representing an enforcement order.
 */
@Builder
@Data
public class EnforcementOrder {

    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @JsonUnwrapped
    @CCD
    private AdditionalInformation additionalInformation;

    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

}
