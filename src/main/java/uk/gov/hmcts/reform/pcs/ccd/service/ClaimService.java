package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ProhibitedConduct;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoToBoolean;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimGroundService claimGroundService;

    public ClaimEntity createMainClaimEntity(PCSCase pcsCase, PartyEntity claimantPartyEntity) {

        String additionalReasons = pcsCase.getAdditionalReasonsForPossession().getReasons();

        List<ClaimGroundEntity> claimGrounds = claimGroundService.getGroundsWithReason(pcsCase);
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();
        SuspensionOfRightToBuy suspensionOrder = resolveSuspensionOfRightToBuy(pcsCase);
        DemotionOfTenancy demotionOrder = resolveDemotionOfTenancy(pcsCase);
        ProhibitedConduct prohibitedConduct = buildProhibitedConduct(pcsCase);

        ClaimEntity claimEntity = ClaimEntity.builder()
            .summary("Main Claim")
            .defendantCircumstances(defendantCircumstances != null
                                        ? defendantCircumstances.getDefendantCircumstancesInfo() : null)
            .suspensionOfRightToBuyHousingAct(suspensionOrder != null
                                                  ? suspensionOrder.getSuspensionOfRightToBuyHousingActs() : null)
            .suspensionOfRightToBuyReason(suspensionOrder != null
                                              ? suspensionOrder.getSuspensionOfRightToBuyReason() : null)
            .demotionOfTenancyHousingAct(demotionOrder != null
                                             ? demotionOrder.getDemotionOfTenancyHousingActs() : null)
            .demotionOfTenancyReason(demotionOrder != null
                                         ? demotionOrder.getDemotionOfTenancyReason() : null)
            .statementOfExpressTermsDetails(demotionOrder != null
                                                ? demotionOrder.getStatementOfExpressTermsDetails() : null)
            .costsClaimed(pcsCase.getClaimingCostsWanted().toBoolean())
            .additionalReasons(additionalReasons)
            .applicationWithClaim(YesOrNoToBoolean.convert(pcsCase.getApplicationWithClaim()))
            .languageUsed(pcsCase.getLanguageUsed())
            .prohibitedConduct(prohibitedConduct)
            .build();


        claimEntity.addParty(claimantPartyEntity, PartyRole.CLAIMANT);
        claimEntity.addClaimGrounds(claimGrounds);
        claimEntity.setClaimantCircumstances(pcsCase.getClaimantCircumstances().getClaimantCircumstancesDetails());
        claimRepository.save(claimEntity);

        return claimEntity;
    }

    private SuspensionOfRightToBuy resolveSuspensionOfRightToBuy(PCSCase pcsCase) {
        SuspensionOfRightToBuy suspension = pcsCase.getSuspensionOfRightToBuy();
        SuspensionOfRightToBuyDemotionOfTenancy combined = pcsCase.getSuspensionOfRightToBuyDemotionOfTenancy();

        if ((suspension == null || suspension.getSuspensionOfRightToBuyHousingActs() == null)
            && combined != null && combined.getSuspensionOfRightToBuyActs() != null) {
            return SuspensionOfRightToBuy.builder()
                .suspensionOfRightToBuyHousingActs(combined.getSuspensionOfRightToBuyActs())
                .suspensionOfRightToBuyReason(combined.getSuspensionOrderReason())
                .build();
        }
        return suspension;
    }

    private DemotionOfTenancy resolveDemotionOfTenancy(PCSCase pcsCase) {
        DemotionOfTenancy demotion = pcsCase.getDemotionOfTenancy();
        SuspensionOfRightToBuyDemotionOfTenancy combined = pcsCase.getSuspensionOfRightToBuyDemotionOfTenancy();

        if ((demotion == null || demotion.getDemotionOfTenancyHousingActs() == null)
            && combined != null && combined.getDemotionOfTenancyActs() != null) {
            return DemotionOfTenancy.builder()
                .demotionOfTenancyHousingActs(combined.getDemotionOfTenancyActs())
                .demotionOfTenancyReason(combined.getDemotionOrderReason())
                .statementOfExpressTermsDetails(demotion != null ? demotion.getStatementOfExpressTermsDetails() : null)
                .build();
        }
        return demotion;
    }

    private ProhibitedConduct buildProhibitedConduct(PCSCase pcsCase) {
        if (pcsCase.getProhibitedConductWalesClaim() == null) {
            return null;
        }

        return ProhibitedConduct.builder()
            .claimForProhibitedConductContract(pcsCase.getProhibitedConductWalesClaim() != null
                ? pcsCase.getProhibitedConductWalesClaim().name() : null)
            .agreedTermsOfPeriodicContract(pcsCase.getProhibitedConductWalesWrappedQuestion() != null
                ? pcsCase.getProhibitedConductWalesWrappedQuestion().getAgreedTermsOfPeriodicContract() != null
                    ? pcsCase.getProhibitedConductWalesWrappedQuestion()
                        .getAgreedTermsOfPeriodicContract().name() : null
                : null)
            .detailsOfTerms(pcsCase.getProhibitedConductWalesWrappedQuestion() != null
                ? pcsCase.getProhibitedConductWalesWrappedQuestion().getDetailsOfTerms() : null)
            .whyMakingClaim(pcsCase.getProhibitedConductWalesWhyMakingClaim())
            .build();
    }


}
