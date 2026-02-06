package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Optional;

@Component
public class AsbProhibitedConductView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getAsbProhibitedConductEntity)
            .ifPresent(asbProhibitedConduct -> setAsbProhibitedConductFields(pcsCase, asbProhibitedConduct));
    }

    private void setAsbProhibitedConductFields(PCSCase pcsCase, AsbProhibitedConductEntity asbProhibitedConductEntity) {
        setAsbQuestions(pcsCase, asbProhibitedConductEntity);
        setProhibitedConductFields(pcsCase, asbProhibitedConductEntity);
    }

    private static void setAsbQuestions(PCSCase pcsCase, AsbProhibitedConductEntity asbProhibitedConductEntity) {
        ASBQuestionsDetailsWales asbDetails = new ASBQuestionsDetailsWales();

        asbDetails.setAntisocialBehaviour(asbProhibitedConductEntity.getAntisocialBehaviour());
        asbDetails.setAntisocialBehaviourDetails(asbProhibitedConductEntity.getAntisocialBehaviourDetails());
        asbDetails.setIllegalPurposesUse(asbProhibitedConductEntity.getIllegalPurposes());
        asbDetails.setIllegalPurposesUseDetails(asbProhibitedConductEntity.getIllegalPurposesDetails());
        asbDetails.setOtherProhibitedConduct(asbProhibitedConductEntity.getOtherProhibitedConduct());
        asbDetails.setOtherProhibitedConductDetails(asbProhibitedConductEntity.getOtherProhibitedConductDetails());

        pcsCase.setAsbQuestionsWales(asbDetails);
    }

    private static void setProhibitedConductFields(PCSCase pcsCase,
                                                   AsbProhibitedConductEntity asbProhibitedConductEntity) {

        pcsCase.setProhibitedConductWalesClaim(asbProhibitedConductEntity.getClaimingStandardContract());
        pcsCase.setProhibitedConductWalesClaimDetails(asbProhibitedConductEntity.getClaimingStandardContractDetails());

        PeriodicContractTermsWales periodicContractTerms = new PeriodicContractTermsWales();
        periodicContractTerms.setAgreedTermsOfPeriodicContract(asbProhibitedConductEntity.getPeriodicContractAgreed());
        periodicContractTerms.setDetailsOfTerms(asbProhibitedConductEntity.getPeriodicContractDetails());

        pcsCase.setPeriodicContractTermsWales(periodicContractTerms);
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

}
