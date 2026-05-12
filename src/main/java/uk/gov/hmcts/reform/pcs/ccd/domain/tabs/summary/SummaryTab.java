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
        label = "Date claim Submitted"
    )
    private String claimSubmittedDate;

    @CCD(
        label = "Reasons for possession"
    )
    private  ReasonsForPossessionTabDetails reasonsForPossession;

    @CCD(
        label = "Date claim Submitted"
    )
    private String possessionReasonsSubmittedDate;

    @CCD(
        label = "Claimant"
    )
    private SummaryClaimantTabDetails claimantDetails;

    @CCD(
        label = "Defendant 1"
    )
    private SummaryDefendantTabDetails defendantDetails;

    @CCD(
        label = "Additional defendant"
    )
    private List<ListValue<SummaryAdditionalDefendantTabDetails>> additionalDefendants;

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
