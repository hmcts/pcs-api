package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CaseDetailsTab {

    @CCD(
        label = "Claim details"
    )
    private ClaimTabDetails claimDetails;

    @CCD(
        label = "Address of property to be repossessed"
    )
    private AddressUK propertyAddress;

    @CCD(
        label = "Grounds for possession"
    )
    private GroundsForPossessionTabDetails groundsForPossessionDetails;

    @CCD(
        label = "Tenancy, occupation contract or licence details"
    )
    private TenancyLicenceTabDetails tenancyLicenceDetails;

    @CCD(
        label = "Notice details"
    )
    private NoticeTabDetails noticeDetails;

    @CCD(
        label = "Actions already taken"
    )
    private ActionsTakenTabDetails actionsTakenDetails;

    @CCD(
        label = "Details of rent arrears"
    )
    private RentArrearsTabDetails rentArrearsDetails;

    @CCD(
        label = "Costs"
    )
    private CostsTabDetails costsDetails;

    @CCD(
        label = "Reasons for possession"
    )
    private ReasonsForPossessionTabDetails reasonsForPossessionDetails;

    @CCD(
        label = "Applications"
    )
    private ApplicationsTabDetails applicationsDetails;

    @CCD(
        label = "Claimant"
    )
    private ClaimantInformationTabDetails claimantInformation;

    @CCD(
        label = "Claimant address for service"
    )
    private AddressUK claimantAddress;

    @CCD(
        label = "Claimant contact details"
    )
    private ClaimantContactTabDetails claimantContactDetails;

    @CCD(
        label = "Claimant circumstances"
    )
    private ClaimantCircumstancesTabDetails claimantCircumstances;

    @CCD(
        label = "Defendant 1"
    )
    private DefendantInformationTabDetails defendantInformationDetails;

    @CCD(
        label = "Defendant 1 address for service"
    )
    private AddressUK defendantOneAddress;

    @CCD(
        label = "Additional defendant"
    )
    private List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants;

    @CCD(
        label = "Defendant’ circumstances"
    )
    private DefendantCircumstanceTabDetails defendantCircumstanceDetails;

    @CCD(
        label = "Underlessee or mortgagee"
    )
    private List<ListValue<UnderlesseeOrMortgageInformationTabDetails>> mortgageDetails;

    @CCD(
        label = "Demotion of tenancy claim"
    )
    private DemotionOfTenancyTabDetails demotionOfTenancyDetails;

    @CCD(
        label = "SuspensionOfRightToBuyTabDetails"
    )
    private SuspensionOfRightToBuyTabDetails suspensionOfRightToBuyDetails;
}
