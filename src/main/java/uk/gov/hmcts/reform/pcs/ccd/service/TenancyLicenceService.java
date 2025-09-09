package uk.gov.hmcts.reform.pcs.ccd.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;

@Service
public class TenancyLicenceService {

    public TenancyLicence buildTenancyLicence(PCSCase pcsCase) {
        return TenancyLicence.builder()
            .noticeServed(toBooleanOrNull(pcsCase.getNoticeServed()))
            .rentAmount(penceToPounds(pcsCase.getCurrentRent()))
            .rentPaymentFrequency(pcsCase.getRentFrequency())
            .otherRentFrequency(pcsCase.getOtherRentFrequency())
            .dailyRentChargeAmount(getDailyRentAmount(pcsCase))
            .totalRentArrears(penceToPounds(pcsCase.getTotalRentArrears()))
            .thirdPartyPaymentSources(pcsCase.getThirdPartyPaymentSources())
            .thirdPartyPaymentSourceOther(pcsCase.getThirdPartyPaymentSourceOther())
            .build();
    }

    private static Boolean toBooleanOrNull(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    private BigDecimal getDailyRentAmount(PCSCase pcsCase) {
        String[] fieldValues = {
            pcsCase.getAmendedDailyRentChargeAmount(),
            pcsCase.getCalculatedDailyRentChargeAmount(),
            pcsCase.getDailyRentChargeAmount()
        };
        for (String value : fieldValues) {
            if (value != null && !value.trim().isEmpty()) {
                return penceToPounds(value);
            }
        }
        return null;
    }

    private static BigDecimal penceToPounds(String pence) {
        if (pence == null || pence.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(pence).movePointLeft(2);
    }
}
