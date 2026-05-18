package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SummaryTab {

    @CCD(
        label = "Address of property to be repossessed"
    )
    private AddressUK repossessedPropertyAddress;

    @CCD(
        label = "Grounds for possession"
    )
    private GroundsForPossessionTabDetails groundsForPossession;

    @CCD(
        label = "Reasons for possession"
    )
    private ReasonsForPossessionTabDetails reasonsForPossession;

    @CCD(
        label = "Date claim Submitted"
    )
    private String dateClaimSubmitted;

    @CCD(
        label = "Claimant"
    )
    private ClaimantInformationTabDetails claimantDetails;

    @CCD(
        label = "Defendant 1"
    )
    private DefendantInformationTabDetails defendantDetails;

    @CCD(
        label = "Additional defendant"
    )
    private List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants;

    @CCD(
        label = "Details of rent arrears"
    )
    private RentArrearsTabDetails rentArrearsDetails;

    @CCD(
        label = "Tenancy, occupation contract or licence details"
    )
    private TenancyTabDetails tenancyDetails;

    @CCD(
        label = "Notice details"
    )
    private NoticeTabDetails noticeDetails;

}
