package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;

import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;

@Component
public class ClaimantInformationTabDetailsBuilder {

    public ClaimantInformationTabDetails createSummaryClaimantTabDetails(PCSCase pcsCase) {
        String claimantName = getSummaryClaimantName(pcsCase);
        if (claimantName == null) {
            return null;
        }

        return ClaimantInformationTabDetails.builder()
            .claimantName(claimantName)
            .build();
    }

    private String getSummaryClaimantName(PCSCase pcsCase) {
        ClaimantInformation claimantInformation = pcsCase.getClaimantInformation();
        if (claimantInformation != null) {
            if (claimantInformation.getOrgNameFound() == NO) {
                return claimantInformation.getFallbackClaimantName();
            }

            if (claimantInformation.getIsClaimantNameCorrect() == VerticalYesNo.NO) {
                return claimantInformation.getOverriddenClaimantName();
            }

            if (claimantInformation.getClaimantName() != null) {
                return claimantInformation.getClaimantName();
            }
        }

        if (CollectionUtils.isEmpty(pcsCase.getAllClaimants())) {
            return null;
        }

        return pcsCase.getAllClaimants().getFirst().getValue().getOrgName();
    }
}
