package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;

@Service
public class PossessionAlternativesService {

    public PossessionAlternativesEntity createPossessionAlternativesEntity(PCSCase pcsCase) {
        Set<AlternativesToPossession> alternativesToPossession = pcsCase.getAlternativesToPossession();

        PossessionAlternativesEntity possessionAlternativesEntity = new PossessionAlternativesEntity();

        if (alternativesToPossession == null) {
            return null;
        }

        boolean suspensionOfRtbRequested = alternativesToPossession.contains(SUSPENSION_OF_RIGHT_TO_BUY);
        boolean demotionOfTenancyRequested = alternativesToPossession.contains(DEMOTION_OF_TENANCY);

        possessionAlternativesEntity.setSuspensionOfRTB(YesOrNo.from(suspensionOfRtbRequested));
        possessionAlternativesEntity.setDotRequested(YesOrNo.from(demotionOfTenancyRequested));

        if (suspensionOfRtbRequested && demotionOfTenancyRequested) {
            setFromCombinedAnswers(pcsCase, possessionAlternativesEntity);
        } else if (suspensionOfRtbRequested) {
            setSuspensionOfRTB(pcsCase, possessionAlternativesEntity);
        } else if (demotionOfTenancyRequested) {
            setDemotionOfTenancy(pcsCase, possessionAlternativesEntity);
        }

        if (demotionOfTenancyRequested) {
            setStatementOfExpress(pcsCase, possessionAlternativesEntity);
        }

        return possessionAlternativesEntity;
    }

    private static void setFromCombinedAnswers(PCSCase pcsCase,
                                               PossessionAlternativesEntity possessionAlternativesEntity) {

        SuspensionOfRightToBuyDemotionOfTenancy combinedAnswers
            = pcsCase.getSuspensionOfRightToBuyDemotionOfTenancy();

        possessionAlternativesEntity
            .setSuspensionOfRTBHousingActSection(combinedAnswers.getSuspensionOfRightToBuyActs());
        possessionAlternativesEntity.setSuspensionOfRTBReason(combinedAnswers.getSuspensionOrderReason());

        possessionAlternativesEntity.setDotHousingActSection(combinedAnswers.getDemotionOfTenancyActs());
        possessionAlternativesEntity.setDotReason(combinedAnswers.getDemotionOrderReason());
    }

    private static void setSuspensionOfRTB(PCSCase pcsCase,
                                           PossessionAlternativesEntity possessionAlternativesEntity) {
        SuspensionOfRightToBuy suspensionOfRightToBuy = pcsCase.getSuspensionOfRightToBuy();
        possessionAlternativesEntity.setSuspensionOfRTBHousingActSection(suspensionOfRightToBuy.getHousingAct());
        possessionAlternativesEntity.setSuspensionOfRTBReason(suspensionOfRightToBuy.getReason());
    }

    private static void setDemotionOfTenancy(PCSCase pcsCase,
                                             PossessionAlternativesEntity possessionAlternativesEntity) {
        DemotionOfTenancy demotionOfTenancy = pcsCase.getDemotionOfTenancy();
        possessionAlternativesEntity.setDotHousingActSection(demotionOfTenancy.getHousingAct());
        possessionAlternativesEntity.setDotReason(demotionOfTenancy.getReason());
    }

    private static void setStatementOfExpress(PCSCase pcsCase,
                                              PossessionAlternativesEntity possessionAlternativesEntity) {
        DemotionOfTenancy demotionOfTenancy = pcsCase.getDemotionOfTenancy();

        VerticalYesNo statementOfExpressTermsServed = demotionOfTenancy.getStatementOfExpressTermsServed();
        if (statementOfExpressTermsServed == VerticalYesNo.YES) {
            possessionAlternativesEntity.setDotStatementServed(YesOrNo.YES);
            possessionAlternativesEntity
                .setDotStatementDetails(demotionOfTenancy.getStatementOfExpressTermsDetails());
        } else {
            possessionAlternativesEntity.setDotStatementServed(YesOrNo.NO);
        }
    }

}
