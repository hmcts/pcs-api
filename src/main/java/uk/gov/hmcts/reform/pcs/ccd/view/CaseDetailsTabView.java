package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.TenancyLicenceTabDetails;

import java.time.format.DateTimeFormatter;

@Component
public class CaseDetailsTabView {

    private static final String NO_ANSWER = " ";
    private static final DateTimeFormatter pattern = DateTimeFormatter.ofPattern("d MMM yyyy");

    public CaseDetailsTab buildCaseDetailsTab(PCSCase pcsCase) {
        ClaimTabDetails claimTabDetails = buildClaimTabDetails(pcsCase);
        GroundsForPossessionTabDetails groundsForPossessionTabDetails = buildGroundsForPossessionTabDetails(pcsCase);
        TenancyLicenceTabDetails tenancyLicenceTabDetails = buildTenancyLicenceTabDetails(pcsCase);

        return CaseDetailsTab.builder()
            .claimDetails(claimTabDetails)
            .propertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossessionDetails(groundsForPossessionTabDetails)
            .tenancyLicenceDetails(tenancyLicenceTabDetails)
            .build();
    }

    private ClaimTabDetails buildClaimTabDetails(PCSCase pcsCase) {
        ClaimantType claimantType = pcsCase.getClaimantType() != null
            ? ClaimantType.fromName(pcsCase.getClaimantType().getValueCode()) : null;

        return ClaimTabDetails.builder()
            .claimantType(claimantType)
            .trespassClaim(pcsCase.getClaimAgainstTrespassers())
            .build();
    }

    private GroundsForPossessionTabDetails buildGroundsForPossessionTabDetails(PCSCase pcsCase) {
        return GroundsForPossessionTabDetails
            .builder()
            .build();
    }

    private TenancyLicenceTabDetails buildTenancyLicenceTabDetails(PCSCase pcsCase) {
        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        if (tenancyLicenceDetails == null) {
            return null;
        }

        //1 Jan 2026 not 2026-2-1

        String tenancyDate = tenancyLicenceDetails.getTenancyLicenceDate() != null ?
            tenancyLicenceDetails.getTenancyLicenceDate().format(pattern): NO_ANSWER;

        return TenancyLicenceTabDetails.builder()
            .typeOfTenancyLicence(tenancyLicenceDetails.getTypeOfTenancyLicence())
            .tenancyLicenceDate(tenancyDate)
            .hasCopyOfTenancyLicence(tenancyLicenceDetails.getHasCopyOfTenancyLicence())
            .tenancyLicenceDocuments(tenancyLicenceDetails.getTenancyLicenceDocuments())
            .reasonsForNoTenancyLicenceDocuments(tenancyLicenceDetails.getReasonsForNoTenancyLicenceDocuments())
            .build();
    }
}
