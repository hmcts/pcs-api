package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimGroundService claimGroundService;

    public ClaimEntity createMainClaimEntity(PCSCase pcsCase, PartyEntity claimantPartyEntity) {

        AdditionalReasons additionalReasons = pcsCase.getAdditionalReasonsForPossession();

        List<ClaimGroundEntity> claimGrounds = claimGroundService.getGroundsWithReason(pcsCase);
        ClaimantCircumstances claimantCircumstances = pcsCase.getClaimantCircumstances();
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .claimantType(pcsCase.getClaimantType() != null
                              ? ClaimantType.fromCode(pcsCase.getClaimantType().getValueCode()) : null)
            .againstTrespassers(pcsCase.getClaimAgainstTrespassers())
            .dueToRentArrears(pcsCase.getClaimDueToRentArrears())
            .claimCosts(pcsCase.getClaimingCostsWanted())
            .preActionProtocolFollowed(pcsCase.getPreActionProtocolCompleted())
            .mediationAttempted(pcsCase.getMediationAttempted())
            .mediationDetails(pcsCase.getMediationAttemptedDetails())
            .settlementAttempted(pcsCase.getSettlementAttempted())
            .settlementDetails(pcsCase.getSettlementAttemptedDetails())
            .claimantCircumstancesProvided(claimantCircumstances != null
                                               ? claimantCircumstances.getClaimantCircumstancesSelect() : null)
            .claimantCircumstances(claimantCircumstances != null
                                       ? claimantCircumstances.getClaimantCircumstancesDetails() : null)
            .additionalDefendants(pcsCase.getAddAnotherDefendant())
            .defendantCircumstancesProvided(defendantCircumstances != null
                                                ? defendantCircumstances.getHasDefendantCircumstancesInfo() : null)
            .defendantCircumstances(defendantCircumstances != null
                                        ? defendantCircumstances.getDefendantCircumstancesInfo() : null)
            .additionalReasonsProvided(additionalReasons != null
                                           ? additionalReasons.getHasReasons() : null)
            .additionalReasons(additionalReasons != null
                                   ? additionalReasons.getReasons() : null)
            .underlesseeOrMortgagee(pcsCase.getHasUnderlesseeOrMortgagee())
            .additionalUnderlesseesOrMortgagees(pcsCase.getAddAdditionalUnderlesseeOrMortgagee())
            .additionalDocsProvided(pcsCase.getWantToUploadDocuments())
            .genAppExpected(pcsCase.getApplicationWithClaim())
            .languageUsed(pcsCase.getLanguageUsed())
            .build();

        claimEntity.addParty(claimantPartyEntity, PartyRole.CLAIMANT);
        claimEntity.addClaimGrounds(claimGrounds);

        claimRepository.save(claimEntity);

        return claimEntity;
    }


}
