package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;

import java.time.LocalDate;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;

/**
 * Defence-specific value formatters not shared with the claim form. The common money/date/address/
 * yes-no helpers live in {@link uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter}.
 */
@Slf4j
final class DefenceFormFormatter {

    private DefenceFormFormatter() {
    }

    // The income/expense/debt rows read "... every: <value>", so the value must be the period noun
    // ("Week"/"Month") the citizen picked, not the enum label ("Weekly"/"Monthly").
    static String formatFrequency(RecurrenceFrequency frequency) {
        if (frequency == null) {
            return null;
        }
        return switch (frequency) {
            case WEEKLY -> "Week";
            case MONTHLY -> "Month";
        };
    }

    // Long date from an ISO date string (as stored in tenancy/notice assertions). Null-safe.
    static String formatIsoDate(String isoDate) {
        if (!isPopulated(isoDate)) {
            return null;
        }
        return formatLongDate(LocalDate.parse(isoDate));
    }

    // Payment-agreement frequency is the raw FE value, not RecurrenceFrequency - map the known ones.
    static String formatAdditionalContributionFrequency(String rawFrequency) {
        if (!isPopulated(rawFrequency)) {
            return null;
        }
        return switch (rawFrequency) {
            case "weekly" -> "Weekly";
            case "every2Weeks" -> "Every 2 weeks";
            case "every4Weeks" -> "Every 4 weeks";
            case "monthly" -> "Monthly";
            default -> {
                // unknown value - render blank rather than leak the raw token
                log.warn("Unmapped instalment contribution frequency '{}'; rendering blank", rawFrequency);
                yield null;
            }
        };
    }
}
