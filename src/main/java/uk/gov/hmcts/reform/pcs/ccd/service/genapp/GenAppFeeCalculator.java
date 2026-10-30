package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;

@Service
@AllArgsConstructor
public class GenAppFeeCalculator {

    private final FeeService feeService;
    private final FeatureToggleService featureToggleService;

    public Optional<FeeDetails> getApplicationFeeDetails(GenAppRequest genAppRequest) {

        boolean paymentNeeded = doesFeeApply(genAppRequest) && !isHwfReferenceProvided(genAppRequest);

        if (!paymentNeeded) {
            return Optional.empty();
        }

        if (genAppRequest.getApplicationType() == GenAppType.SUSPEND) {
            return Optional.of(feeService.getFee(FeeType.GEN_APP_SUSPEND_FEE));
        }

        VerticalYesNo otherPartiesAgreed = genAppRequest.getOtherPartiesAgreed();
        FeeType feeType;
        if (otherPartiesAgreed == VerticalYesNo.YES) {
            feeType = FeeType.GEN_APP_STANDARD_FEE;
        } else {
            if (genAppRequest.getWithoutNotice() == VerticalYesNo.YES) {
                feeType = FeeType.GEN_APP_STANDARD_FEE;
            } else {
                feeType = FeeType.GEN_APP_MAX_FEE;
            }
        }

        return Optional.of(feeService.getFee(feeType));
    }

    private boolean doesFeeApply(GenAppRequest genAppRequest) {
        GenAppType applicationType = genAppRequest.getApplicationType();

        return (applicationType != GenAppType.ADJOURN)
            || (genAppRequest.getWithin14Days() == VerticalYesNo.YES)
            || (featureToggleService.isEnabled(FeatureFlag.GENAPP_CUI_SET_ASIDE)
                && (genAppRequest.getOtherPartiesAgreed() == VerticalYesNo.NO));
    }

    private static boolean isHwfReferenceProvided(GenAppRequest genAppRequest) {
        return genAppRequest.getNeedHwf() == VerticalYesNo.YES
            && genAppRequest.getAppliedForHwf() == VerticalYesNo.YES
            && StringUtils.isNotBlank(genAppRequest.getHwfReference());
    }

}
