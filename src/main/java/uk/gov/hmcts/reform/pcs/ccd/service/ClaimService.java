package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.utils.YesOrNoToBoolean;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimGroundService claimGroundService;

    public ClaimEntity createMainClaimEntity(PCSCase pcsCase, PartyEntity claimantPartyEntity) {

        String additionalReasons = pcsCase.getAdditionalReasonsForPossession().getReasons();

        SuspensionOfRightToBuyDemotionOfTenancy suspensionAndDemotion = pcsCase
                                                                          .getSuspensionOfRightToBuyDemotionOfTenancy();
        SuspensionOfRightToBuy suspensionOfRightToBuy = pcsCase.getSuspensionOfRightToBuy();
        DemotionOfTenancy demotionOfTenancy = pcsCase.getDemotionOfTenancy();

        List<ClaimGroundEntity> claimGrounds = claimGroundService.getGroundsWithReason(pcsCase);
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .summary("Main Claim")
            .defendantCircumstances(defendantCircumstances != null
                                        ? defendantCircumstances.getDefendantCircumstancesInfo() : null)
            .suspensionOfRightToBuyHousingAct(suspensionOfRightToBuy.getSuspensionOfRightToBuyHousingActs() != null
                                                  ? suspensionOfRightToBuy.getSuspensionOfRightToBuyHousingActs()
                                                  : suspensionAndDemotion.getSuspensionOfRightToBuyActs())
            .suspensionOfRightToBuyReason(suspensionOfRightToBuy.getSuspensionOfRightToBuyReason() != null
                                              ? suspensionOfRightToBuy.getSuspensionOfRightToBuyReason()
                                              : suspensionAndDemotion.getSuspensionOrderReason())
            .demotionOfTenancyHousingAct(demotionOfTenancy.getDemotionOfTenancyHousingActs() != null
                                             ? demotionOfTenancy.getDemotionOfTenancyHousingActs()
                                             : suspensionAndDemotion.getDemotionOfTenancyActs())
            .demotionOfTenancyReason(demotionOfTenancy.getDemotionOfTenancyReason() != null
                                         ? demotionOfTenancy.getDemotionOfTenancyReason()
                                         : suspensionAndDemotion.getDemotionOrderReason())
            .statementOfExpressTermsDetails(demotionOfTenancy.getStatementOfExpressTermsDetails() != null
                                                ? demotionOfTenancy.getStatementOfExpressTermsDetails()
                                                : suspensionAndDemotion.getExpressTermsDetails())
            .costsClaimed(pcsCase.getClaimingCostsWanted().toBoolean())
            .additionalReasons(additionalReasons)
            .applicationWithClaim(YesOrNoToBoolean.convert(pcsCase.getApplicationWithClaim()))
            .build();

        claimEntity.addParty(claimantPartyEntity, PartyRole.CLAIMANT);
        claimEntity.addClaimGrounds(claimGrounds);
        claimEntity.setClaimantCircumstances(pcsCase.getClaimantCircumstances().getClaimantCircumstancesDetails());
        claimRepository.save(claimEntity);

        return claimEntity;
    }

}
