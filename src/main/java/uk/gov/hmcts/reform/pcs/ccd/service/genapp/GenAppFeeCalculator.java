package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@AllArgsConstructor
public class GenAppFeeCalculator {

    private final FeeService feeService;

    public Optional<BigDecimal> getApplicationFee(GenAppRequest genAppRequest) {

        boolean paymentNeeded = doesFeeApply(genAppRequest) && !isHwfReferenceProvided(genAppRequest);

        if (!paymentNeeded) {
            return Optional.empty();
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

        BigDecimal feeAmount = feeService.getFee(feeType).getFeeAmount();
        return Optional.of(feeAmount);
    }

    private static boolean doesFeeApply(GenAppRequest genAppRequest) {
        GenAppType applicationType = genAppRequest.getApplicationType();

        return (applicationType != GenAppType.ADJOURN)
            || (genAppRequest.getWithin14Days() == VerticalYesNo.YES);
    }

    private static boolean isHwfReferenceProvided(GenAppRequest genAppRequest) {
        return genAppRequest.getNeedHwf() == VerticalYesNo.YES
            && genAppRequest.getAppliedForHwf() == VerticalYesNo.YES
            && StringUtils.isNotBlank(genAppRequest.getHwfReference());
    }

}
