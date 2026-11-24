package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsRentedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.PaymentHistoryType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ValueFrequencyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.DailyRentCalculator;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Maps the rent and rent arrears sections. They are mandatory only when the grounds claimed involve rent arrears,
 * but any rent data supplied is stored either way. The rent statement document is never required on this path.
 */
@Component
public class RentalRentMapper {

    private final DailyRentCalculator dailyRentCalculator;

    public RentalRentMapper(DailyRentCalculator dailyRentCalculator) {
        this.dailyRentCalculator = dailyRentCalculator;
    }

    /**
     * Maps the rent and arrears sections. When the sections do not apply, whatever rent data the claim supplies is
     * still mapped and validated, but nothing is required.
     *
     * @param rentApplies whether the grounds claimed make the rent sections applicable
     * @param path path of the particulars element relative to the claim
     */
    public void apply(ParticularsRentedType particulars,
                      boolean rentApplies,
                      PCSCase target,
                      String path,
                      BulkErrorCollector errors) {

        target.setShowRentSectionPage(YesOrNo.from(rentApplies));
        if (!rentApplies && !hasRentData(particulars)) {
            target.setRentArrears(RentArrearsSection.builder().build());
            return;
        }

        RentDetails details = rentDetails(particulars, rentApplies, path, errors);
        if (details != null) {
            target.setRentDetails(details);
            target.setRentSectionPaymentFrequency(details.getFrequency());
        }
        target.setRentArrears(rentArrears(particulars, rentApplies, path, errors));
        if (rentApplies || particulars.isHasExpenseClaim() != null) {
            target.setArrearsJudgmentWanted(
                VerticalYesNo.from(Boolean.TRUE.equals(particulars.isHasExpenseClaim())));
        }
    }

    private static boolean hasRentData(ParticularsRentedType particulars) {
        return particulars.getCurrentRent() != null
            || particulars.getDailyRent() != null
            || particulars.getTotalArrears() != null
            || StringUtils.isNotBlank(particulars.getRecoverySteps())
            || particulars.isHasExpenseClaim() != null
            || (particulars.getPaymentHistory() != null
                && !particulars.getPaymentHistory().getPaymentLine().isEmpty());
    }

    private RentDetails rentDetails(ParticularsRentedType particulars, boolean mandatory, String path,
                                    BulkErrorCollector errors) {
        ValueFrequencyType rent = particulars.getCurrentRent();
        if (rent == null) {
            if (mandatory) {
                errors.add(path + "/CurrentRent", BulkErrorCollector.REQUIRED_CODE,
                           "Current rent is required when rent arrears are claimed");
                return RentDetails.builder().build();
            }
            return null;
        }

        RentPaymentFrequency frequency = switch (rent.getFrequency()) {
            case WEEK -> RentPaymentFrequency.WEEKLY;
            case FORTNIGHT -> RentPaymentFrequency.FORTNIGHTLY;
            case MONTH -> RentPaymentFrequency.MONTHLY;
            case QUARTER, YEAR, OTHER -> RentPaymentFrequency.OTHER;
        };

        RentDetails details = RentDetails.builder()
            .currentRent(rent.getAmount())
            .frequency(frequency)
            .build();
        BigDecimal suppliedDaily = particulars.getDailyRent();

        if (frequency == RentPaymentFrequency.OTHER) {
            details.setOtherFrequency(otherFrequency(rent, path, errors));
            if (!mandatory || errors.required(path + "/DailyRent", suppliedDaily,
                                "Daily rent is required when rent is not paid weekly, fortnightly or monthly")) {
                details.setDailyCharge(suppliedDaily);
            }
        } else {
            dailyRentCalculator.applyCalculatedDailyCharge(details);
            boolean matches = suppliedDaily == null
                || suppliedDaily.compareTo(details.getCalculatedDailyCharge()) == 0;
            details.setPerDayCorrect(VerticalYesNo.from(matches));
            if (!matches) {
                details.setAmendedDailyCharge(suppliedDaily);
            }
        }
        return details;
    }

    private String otherFrequency(ValueFrequencyType rent, String path, BulkErrorCollector errors) {
        String description = StringUtils.trimToNull(rent.getOtherFrequencyDescription());
        String frequencyPath = path + "/CurrentRent/OtherFrequencyDescription";
        String text = switch (rent.getFrequency()) {
            case QUARTER -> StringUtils.defaultIfBlank(description, "Quarterly");
            case YEAR -> StringUtils.defaultIfBlank(description, "Yearly");
            default -> description;
        };
        if (errors.required(frequencyPath, text, "Description of how often the rent is paid")) {
            errors.maxLength(frequencyPath, text, BulkLimits.NAME, "Rent frequency");
        }
        return text;
    }

    private RentArrearsSection rentArrears(ParticularsRentedType particulars, boolean mandatory, String path,
                                           BulkErrorCollector errors) {
        BigDecimal total = particulars.getTotalArrears() != null
            ? particulars.getTotalArrears() : latestCumulativeArrears(particulars.getPaymentHistory());
        if (mandatory) {
            errors.required(path + "/TotalArrears", total,
                            "Total arrears (or a payment history to derive it from) is required");
        }

        String recoverySteps = StringUtils.trimToNull(particulars.getRecoverySteps());
        errors.maxLength(path + "/RecoverySteps", recoverySteps, BulkLimits.MEDIUM_TEXT,
                         RentArrearsSection.RECOVERY_ATTEMPT_DETAILS_LABEL);

        return RentArrearsSection.builder()
            .total(total)
            .recoveryAttempted(VerticalYesNo.from(recoverySteps != null))
            .recoveryAttemptDetails(recoverySteps)
            .build();
    }

    private static BigDecimal latestCumulativeArrears(PaymentHistoryType history) {
        if (history == null || history.getPaymentLine().isEmpty()) {
            return null;
        }
        return history.getPaymentLine().stream()
            .max(Comparator.comparing(PaymentHistoryType.PaymentLine::getPaymentDueDate))
            .map(PaymentHistoryType.PaymentLine::getCumulativeArrears)
            .orElse(null);
    }
}
