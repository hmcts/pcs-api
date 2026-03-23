package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;

@Service
public class AsbProhibitedConductService {

    public AsbProhibitedConductEntity createAsbProhibitedConductEntity(PCSCase pcsCase) {
        ASBQuestionsDetailsWales asbQuestionsWales = pcsCase.getAsbQuestionsWales();

        VerticalYesNo prohibitedConductWalesClaim = pcsCase.getProhibitedConductWalesClaim();

        boolean hasAsbDetails = (asbQuestionsWales != null && asbQuestionsWales.getAntisocialBehaviour() != null);
        boolean hasProhibitedConductClaimDetails = (prohibitedConductWalesClaim != null);

        if (!hasAsbDetails && !hasProhibitedConductClaimDetails) {
            return null;
        }

        AsbProhibitedConductEntity asbProhibitedConductEntity = new AsbProhibitedConductEntity();

        if (hasAsbDetails) {
            VerticalYesNo antisocialBehaviour = asbQuestionsWales.getAntisocialBehaviour();
            asbProhibitedConductEntity.setAntisocialBehaviour(antisocialBehaviour);
            if (antisocialBehaviour == VerticalYesNo.YES) {
                asbProhibitedConductEntity.setAntisocialBehaviourDetails(
                    asbQuestionsWales.getAntisocialBehaviourDetails()
                );
            }

            VerticalYesNo illegalPurposes = asbQuestionsWales.getIllegalPurposesUse();
            asbProhibitedConductEntity.setIllegalPurposes(illegalPurposes);
            if (illegalPurposes == VerticalYesNo.YES) {
                asbProhibitedConductEntity.setIllegalPurposesDetails(
                    asbQuestionsWales.getIllegalPurposesUseDetails()
                );
            }

            VerticalYesNo otherProhibitedConduct = asbQuestionsWales.getOtherProhibitedConduct();
            asbProhibitedConductEntity.setOtherProhibitedConduct(otherProhibitedConduct);
            if (otherProhibitedConduct == VerticalYesNo.YES) {
                asbProhibitedConductEntity.setOtherProhibitedConductDetails(
                    asbQuestionsWales.getOtherProhibitedConductDetails()
                );
            }

        }

        if (hasProhibitedConductClaimDetails) {
            asbProhibitedConductEntity.setClaimingStandardContract(prohibitedConductWalesClaim);
            if (prohibitedConductWalesClaim == VerticalYesNo.YES) {
                asbProhibitedConductEntity.setClaimingStandardContractDetails(
                    pcsCase.getProhibitedConductWalesClaimDetails()
                );

                PeriodicContractTermsWales periodicContractTermsWales = pcsCase.getPeriodicContractTermsWales();
                VerticalYesNo periodicContractAgreed = periodicContractTermsWales.getAgreedTermsOfPeriodicContract();
                asbProhibitedConductEntity.setPeriodicContractAgreed(periodicContractAgreed);
                if (periodicContractAgreed == VerticalYesNo.YES) {
                    asbProhibitedConductEntity.setPeriodicContractDetails(
                        periodicContractTermsWales.getDetailsOfTerms()
                    );
                }
            }
        }

        return asbProhibitedConductEntity;
    }

}
