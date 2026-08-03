package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;
import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toVerticalYesNo;

@Component
public class AlternativesToPossessionView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getPossessionAlternativesEntity)
            .ifPresent(possessionAlternatives -> setPossessionAlternativesFields(pcsCase, possessionAlternatives));
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

    private static void setPossessionAlternativesFields(PCSCase pcsCase,
                                                        PossessionAlternativesEntity possessionAlternatives) {

        Set<AlternativesToPossession> alternativesToPossession = new HashSet<>();

        if (possessionAlternatives.getSuspensionOfRTB() == YesOrNo.YES) {
            SuspensionOfRightToBuy suspensionOfRightToBuy = SuspensionOfRightToBuy.builder()
                .housingAct(possessionAlternatives.getSuspensionOfRTBHousingActSection())
                .reason(possessionAlternatives.getSuspensionOfRTBReason())
                .build();
            pcsCase.setSuspensionOfRightToBuy(suspensionOfRightToBuy);
            alternativesToPossession.add(SUSPENSION_OF_RIGHT_TO_BUY);
        }

        if (possessionAlternatives.getDotRequested() == YesOrNo.YES) {
            DemotionOfTenancy demotionOfTenancy = DemotionOfTenancy.builder()
                .housingAct(possessionAlternatives.getDotHousingActSection())
                .reason(possessionAlternatives.getDotReason())
                .statementOfExpressTermsServed(
                    toVerticalYesNo(possessionAlternatives.getDotStatementServed()))
                .statementOfExpressTermsDetails(possessionAlternatives.getDotStatementDetails())
                .build();
            pcsCase.setDemotionOfTenancy(demotionOfTenancy);
            alternativesToPossession.add(DEMOTION_OF_TENANCY);
        }

        pcsCase.setAlternativesToPossession(alternativesToPossession);
    }

}
