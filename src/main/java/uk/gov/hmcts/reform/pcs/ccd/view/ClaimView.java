package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

@Component
public class ClaimView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (!pcsCaseEntity.getClaims().isEmpty()) {
            ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
            mapBasicClaimFields(pcsCase, mainClaim);
            mapComplexClaimFields(pcsCase, mainClaim);
        }
    }

    private void mapBasicClaimFields(PCSCase pcsCase, ClaimEntity claim) {
        pcsCase.setClaimAgainstTrespassers(claim.getAgainstTrespassers());
        pcsCase.setClaimDueToRentArrears(claim.getDueToRentArrears());
        pcsCase.setClaimingCostsWanted(claim.getClaimCosts());
        pcsCase.setPreActionProtocolCompleted(claim.getPreActionProtocolFollowed());
        pcsCase.setMediationAttempted(claim.getMediationAttempted());
        pcsCase.setMediationAttemptedDetails(claim.getMediationDetails());
        pcsCase.setSettlementAttempted(claim.getSettlementAttempted());
        pcsCase.setSettlementAttemptedDetails(claim.getSettlementDetails());
        pcsCase.setAddAnotherDefendant(claim.getAdditionalDefendants());
        pcsCase.setHasUnderlesseeOrMortgagee(claim.getUnderlesseeOrMortgagee());
        pcsCase.setAddAdditionalUnderlesseeOrMortgagee(claim.getAdditionalUnderlesseesOrMortgagees());
        pcsCase.setApplicationWithClaim(claim.getGenAppExpected());
        pcsCase.setLanguageUsed(claim.getLanguageUsed());
        pcsCase.setWantToUploadDocuments(claim.getAdditionalDocsProvided());
    }

    private void mapComplexClaimFields(PCSCase pcsCase, ClaimEntity claim) {
        pcsCase.setClaimantCircumstances(
            ClaimantCircumstances.builder()
                .claimantCircumstancesSelect(claim.getClaimantCircumstancesProvided())
                .claimantCircumstancesDetails(claim.getClaimantCircumstances())
                .build()
        );

        pcsCase.setDefendantCircumstances(
            DefendantCircumstances.builder()
                .hasDefendantCircumstancesInfo(claim.getDefendantCircumstancesProvided())
                .defendantCircumstancesInfo(claim.getDefendantCircumstances())
                .build()
        );

        pcsCase.setAdditionalReasonsForPossession(
            AdditionalReasons.builder()
                .hasReasons(claim.getAdditionalReasonsProvided())
                .reasons(claim.getAdditionalReasons())
                .build()
        );

        if (claim.getClaimantType() != null) {
            pcsCase.setClaimantType(DynamicStringList.builder()
                                        .value(DynamicStringListElement.builder().code(claim.getClaimantType().name())
                                                   .label(claim.getClaimantType().getLabel())
                                                   .build())
                                        .build());
        }

    }

}
