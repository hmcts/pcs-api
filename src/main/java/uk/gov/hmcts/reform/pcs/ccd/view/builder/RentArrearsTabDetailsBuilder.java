package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;

import java.math.BigDecimal;

@Component
public class RentArrearsTabDetailsBuilder {

    public RentArrearsTabDetails buildRentArrearsTabDetails(PCSCase pcsCase) {
        RentDetails rentDetails = pcsCase.getRentDetails();
        RentArrearsSection rentArrears = pcsCase.getRentArrears();

        String rentAmount = rentDetails == null ? null : formatMoney(rentDetails.getCurrentRent());
        String calculationFrequency = getRentCalculationFrequency(rentDetails);
        String dailyRate = getDailyRate(rentDetails);
        String arrearsTotal = rentArrears == null ? null : formatMoney(rentArrears.getTotal());
        String judgmentRequested = pcsCase.getArrearsJudgmentWanted() == null
            ? null : pcsCase.getArrearsJudgmentWanted().getLabel();

        if (rentAmount == null
            && calculationFrequency == null
            && dailyRate == null
            && arrearsTotal == null
            && judgmentRequested == null) {
            return null;
        }

        return RentArrearsTabDetails.builder()
            .rentAmount(rentAmount)
            .calculationFrequency(calculationFrequency)
            .dailyRate(dailyRate)
            .arrearsTotal(arrearsTotal)
            .judgmentRequested(judgmentRequested)
            .build();
    }

    private String getDailyRate(RentDetails rentDetails) {
        if (rentDetails == null) {
            return null;
        }

        if (rentDetails.getPerDayCorrect() == VerticalYesNo.NO && rentDetails.getAmendedDailyCharge() != null) {
            return formatMoney(rentDetails.getAmendedDailyCharge());
        }

        if (rentDetails.getDailyCharge() != null) {
            return formatMoney(rentDetails.getDailyCharge());
        }

        if (rentDetails.getFormattedCalculatedDailyCharge() != null) {
            return rentDetails.getFormattedCalculatedDailyCharge();
        }

        return formatMoney(rentDetails.getCalculatedDailyCharge());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        if (amount.stripTrailingZeros().scale() <= 0) {
            amount = amount.stripTrailingZeros();
        }

        return "£" + amount.toPlainString();
    }

    private String getRentCalculationFrequency(RentDetails rentDetails) {
        if (rentDetails == null || rentDetails.getFrequency() == null) {
            return null;
        }

        if (rentDetails.getFrequency() == RentPaymentFrequency.OTHER && rentDetails.getOtherFrequency() != null) {
            return rentDetails.getOtherFrequency();
        }

        return rentDetails.getFrequency().getLabel();
    }
}
