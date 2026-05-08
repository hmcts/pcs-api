package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SummaryTab {

    @CCD(
        label = "Address of property to be repossessed"
    )
    private AddressUK addressOfPropertyToBeRepossessed;

    @CCD(
        label = "Grounds for possession"
    )
    private GroundsForPossessionTabDetails groundsForPossession;

    @CCD(
        label = "Date claim Submitted"
    )
    private String dateSubmitted;

    @CCD(
        label = "Reasons for possession"
    )
    private  ReasonsForPossessionTabDetails reasonsForPossession;

    @CCD(
        label = "Date claim Submitted"
    )
    private String reasonsDateSubmitted;

    @CCD(
        label = "Claimant Details"
    )
    private SummaryClaimantTabDetails summaryClaimantTabDetails;

//    @CCD(
//        label = "Defendant 1"
//    )
//    private SummaryDefendantTabDetails defendantTabDetails;
//
//    @CCD(
//        label = "Defendant 1"
//    )
//    private SummaryDefendantAddressTabDetails defendantTabDetails;

}
