package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RentArrearsTabDetailsBuilder {

    private static final String NO_ANSWER = " ";

    public RentArrearsTabDetails buildRentArrearsTabDetails(PCSCase pcsCase) {
        if (hasCurrentGroundSummariesWithoutRentArrears(pcsCase)) {
            return null;
        }

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

    private boolean hasCurrentGroundSummariesWithoutRentArrears(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getClaimGroundSummaries())) {
            return false;
        }

        boolean hasRentArrearsFlag = false;
        for (ListValue<ClaimGroundSummary> summaryListValue : pcsCase.getClaimGroundSummaries()) {
            ClaimGroundSummary summary = summaryListValue.getValue();
            if (summary == null || summary.getIsRentArrears() == null) {
                continue;
            }

            hasRentArrearsFlag = true;
            if (summary.getIsRentArrears() == YesOrNo.YES) {
                return false;
            }
        }

        return hasRentArrearsFlag;
    }

    public RentArrearsTabDetails buildDetailedRentArrearsTabDetails(PCSCase pcsCase) {
        if (pcsCase.getShowRentSectionPage() != YesOrNo.YES) {
            return null;
        }

        String judgmentRequested = pcsCase.getArrearsJudgmentWanted() == null
            ? NO_ANSWER : pcsCase.getArrearsJudgmentWanted().getLabel();

        RentArrearsTabDetails rentArrearsTabDetails;
        RentDetails rentDetails = pcsCase.getRentDetails();
        RentArrearsSection rentArrears = pcsCase.getRentArrears();

        if (rentDetails != null) {
            String rentAmount = formatMoney(rentDetails.getCurrentRent());
            String calculationFrequency = getRentCalculationFrequencyDetailed(rentDetails);
            String dailyRate = getDailyRate(rentDetails);

            String frequency = null;
            if (rentDetails.getFrequency() == RentPaymentFrequency.OTHER) {
                frequency = rentDetails.getOtherFrequency();
            }

            rentArrearsTabDetails = RentArrearsTabDetails.builder()
                .rentAmount(rentAmount != null ? rentAmount : NO_ANSWER)
                .calculationFrequency(calculationFrequency != null ? calculationFrequency : NO_ANSWER)
                .dailyRate(dailyRate != null ? dailyRate : NO_ANSWER)
                .judgmentRequested(judgmentRequested)
                .frequency(frequency)
                .build();
        } else {
            rentArrearsTabDetails = RentArrearsTabDetails.builder()
                .rentAmount(NO_ANSWER)
                .calculationFrequency(NO_ANSWER)
                .dailyRate(NO_ANSWER)
                .judgmentRequested(judgmentRequested)
                .build();
        }

        if (rentArrears != null) {
            VerticalYesNo recoveryAttempted = rentArrears.getRecoveryAttempted();
            rentArrearsTabDetails
                .setStepsToRecoverArrears(recoveryAttempted != null ? recoveryAttempted.getLabel() : NO_ANSWER);

            if (recoveryAttempted == VerticalYesNo.YES) {
                String details = rentArrears.getRecoveryAttemptDetails();
                rentArrearsTabDetails.setStepsToRecoverArrearsDetails(details != null ? details : NO_ANSWER);
            }

            String arrearsTotal = formatMoney(rentArrears.getTotal());
            rentArrearsTabDetails.setArrearsTotal(arrearsTotal != null ? arrearsTotal : NO_ANSWER);

            List<ListValue<Document>> documents = rentArrears.getStatementDocuments();
            if (CollectionUtils.isEmpty(documents)) {
                rentArrearsTabDetails.setRentStatementPlaceholder(NO_ANSWER);
            } else {
                rentArrearsTabDetails.setRentStatement(documents);
            }
        } else {
            rentArrearsTabDetails.setStepsToRecoverArrears(NO_ANSWER);
            rentArrearsTabDetails.setRentStatementPlaceholder(NO_ANSWER);
            rentArrearsTabDetails.setArrearsTotal(NO_ANSWER);
        }

        return rentArrearsTabDetails;
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

    private String getRentCalculationFrequencyDetailed(RentDetails rentDetails) {
        if (rentDetails == null || rentDetails.getFrequency() == null) {
            return null;
        }

        return rentDetails.getFrequency().getLabel();
    }
}
