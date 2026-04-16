package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;

@Service
public class AsbProhibitedConductService {

    public AsbProhibitedConductEntity createAsbProhibitedConductEntity(PCSCase pcsCase) {
        ASBQuestionsDetailsWales asbQuestionsWales = pcsCase.getAsbQuestionsWales();

        SimpleYesNo prohibitedConductWalesClaim = pcsCase.getProhibitedConductWalesClaim();

        boolean hasAsbDetails = (asbQuestionsWales != null && asbQuestionsWales.getAntisocialBehaviour() != null);
        boolean hasProhibitedConductClaimDetails = (prohibitedConductWalesClaim != null);

        if (!hasAsbDetails && !hasProhibitedConductClaimDetails) {
            return null;
        }

        AsbProhibitedConductEntity asbProhibitedConductEntity = new AsbProhibitedConductEntity();

        if (hasAsbDetails) {
            SimpleYesNo antisocialBehaviour = asbQuestionsWales.getAntisocialBehaviour();
            asbProhibitedConductEntity.setAntisocialBehaviour(antisocialBehaviour);
            if (antisocialBehaviour == SimpleYesNo.YES) {
                asbProhibitedConductEntity.setAntisocialBehaviourDetails(
                    asbQuestionsWales.getAntisocialBehaviourDetails()
                );
            }

            SimpleYesNo illegalPurposes = asbQuestionsWales.getIllegalPurposesUse();
            asbProhibitedConductEntity.setIllegalPurposes(illegalPurposes);
            if (illegalPurposes == SimpleYesNo.YES) {
                asbProhibitedConductEntity.setIllegalPurposesDetails(
                    asbQuestionsWales.getIllegalPurposesUseDetails()
                );
            }

            SimpleYesNo otherProhibitedConduct = asbQuestionsWales.getOtherProhibitedConduct();
            asbProhibitedConductEntity.setOtherProhibitedConduct(otherProhibitedConduct);
            if (otherProhibitedConduct == SimpleYesNo.YES) {
                asbProhibitedConductEntity.setOtherProhibitedConductDetails(
                    asbQuestionsWales.getOtherProhibitedConductDetails()
                );
            }

        }

        if (hasProhibitedConductClaimDetails) {
            asbProhibitedConductEntity.setClaimingStandardContract(prohibitedConductWalesClaim);
            if (prohibitedConductWalesClaim == SimpleYesNo.YES) {
                asbProhibitedConductEntity.setClaimingStandardContractDetails(
                    pcsCase.getProhibitedConductWalesClaimDetails()
                );

                PeriodicContractTermsWales periodicContractTermsWales = pcsCase.getPeriodicContractTermsWales();
                SimpleYesNo periodicContractAgreed = periodicContractTermsWales.getAgreedTermsOfPeriodicContract();
                asbProhibitedConductEntity.setPeriodicContractAgreed(periodicContractAgreed);
                if (periodicContractAgreed == SimpleYesNo.YES) {
                    asbProhibitedConductEntity.setPeriodicContractDetails(
                        periodicContractTermsWales.getDetailsOfTerms()
                    );
                }
            }
        }

        return asbProhibitedConductEntity;
    }

}
