package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoToBoolean;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimGroundService claimGroundService;

    public ClaimEntity createMainClaimEntity(PCSCase pcsCase) {

        String additionalReasons = pcsCase.getAdditionalReasonsForPossession().getReasons();

        List<ClaimGroundEntity> claimGrounds = claimGroundService.getGroundsWithReason(pcsCase);
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();
        SuspensionOfRightToBuy suspensionOrder = resolveSuspensionOfRightToBuy(pcsCase);
        DemotionOfTenancy demotionOrder = resolveDemotionOfTenancy(pcsCase);
        ProhibitedConductWales prohibitedConduct = buildProhibitedConduct(pcsCase);
        ASBQuestionsWales asbQuestions = buildAsbQuestions(pcsCase);

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
            .asbQuestions(asbQuestions)
            .build();


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

    private ProhibitedConductWales buildProhibitedConduct(PCSCase pcsCase) {
        if (pcsCase.getProhibitedConductWalesClaim() == null) {
            return null;
        }

        return ProhibitedConductWales.builder()
            .claimForProhibitedConductContract(YesOrNoToBoolean.convert(pcsCase.getProhibitedConductWalesClaim()))
            .agreedTermsOfPeriodicContract(pcsCase.getPeriodicContractTermsWales() != null
                ? YesOrNoToBoolean.convert(pcsCase.getPeriodicContractTermsWales().getAgreedTermsOfPeriodicContract())
                : null)
            .detailsOfTerms(pcsCase.getPeriodicContractTermsWales() != null
                ? pcsCase.getPeriodicContractTermsWales().getDetailsOfTerms() : null)
            .whyMakingClaim(pcsCase.getProhibitedConductWalesWhyMakingClaim())
            .build();
    }

    private ASBQuestionsWales buildAsbQuestions(PCSCase pcsCase) {
        if (pcsCase.getAsbQuestionsWales() == null) {
            return null;
        }

        return ASBQuestionsWales.builder()
            .antisocialBehaviour(YesOrNoToBoolean.convert(
                pcsCase.getAsbQuestionsWales().getAntisocialBehaviour()))
            .antisocialBehaviourDetails(pcsCase.getAsbQuestionsWales().getAntisocialBehaviourDetails())
            .illegalPurposesUse(YesOrNoToBoolean.convert(
                pcsCase.getAsbQuestionsWales().getIllegalPurposesUse()))
            .illegalPurposesUseDetails(pcsCase.getAsbQuestionsWales().getIllegalPurposesUseDetails())
            .otherProhibitedConduct(YesOrNoToBoolean.convert(
                pcsCase.getAsbQuestionsWales().getOtherProhibitedConduct()))
            .otherProhibitedConductDetails(pcsCase.getAsbQuestionsWales().getOtherProhibitedConductDetails())
            .build();
    }

}
