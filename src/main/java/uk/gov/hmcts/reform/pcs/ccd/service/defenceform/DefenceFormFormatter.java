package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value formatting helpers specific to the defence-form payload mapping, kept out of
 * {@link DefenceFormPayloadBuilder}. Shared money/date/address/yes-no helpers delegate to
 * {@link FormFieldFormatter}; only the defence-specific overloads live here. Money is held in pounds
 * in the entities, so {@link FormFieldFormatter#formatGbp} does no division.
 */
@Slf4j
final class DefenceFormFormatter {

    private DefenceFormFormatter() {
    }

    static boolean isPopulated(String text) {
        return FormFieldFormatter.isPopulated(text);
    }

    static boolean isYes(VerticalYesNo yesNo) {
        return FormFieldFormatter.isYes(yesNo);
    }

    static boolean isYes(YesNoNotSure yesNo) {
        return yesNo == YesNoNotSure.YES;
    }

    static boolean isNo(VerticalYesNo yesNo) {
        return FormFieldFormatter.isNo(yesNo);
    }

    static boolean isNo(YesNoNotSure yesNo) {
        return yesNo == YesNoNotSure.NO;
    }

    static String toLabel(VerticalYesNo yesNo) {
        return FormFieldFormatter.toLabel(yesNo);
    }

    static String toLabel(YesNoNotSure yesNo) {
        return yesNo == null ? null : yesNo.getLabel();
    }

    static String formatFrequency(RecurrenceFrequency frequency) {
        return frequency == null ? null : frequency.getLabel();
    }

    static String formatGbp(BigDecimal amount) {
        return FormFieldFormatter.formatGbp(amount);
    }

    static String formatLongDate(LocalDate date) {
        return FormFieldFormatter.formatLongDate(date);
    }

    // Long date from an ISO date string (as stored in tenancy/notice assertions). Null-safe.
    static String formatIsoDate(String isoDate) {
        if (!isPopulated(isoDate)) {
            return null;
        }
        return formatLongDate(LocalDate.parse(isoDate));
    }

    // The payment-agreement frequency is stored as the raw FE value rather than RecurrenceFrequency,
    // so map its four known values to display labels.
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
                // Don't leak an unmapped internal FE token onto the rendered form; hide it and flag it.
                log.warn("Unmapped instalment contribution frequency '{}'; rendering blank", rawFrequency);
                yield null;
            }
        };
    }

    static ClaimFormAddress toFormAddress(AddressEntity address) {
        return FormFieldFormatter.toFormAddress(address);
    }

    static ClaimFormAddress toFormAddress(AddressUK address) {
        if (address == null) {
            return null;
        }
        return ClaimFormAddress.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postcode(address.getPostCode())
            .country(address.getCountry())
            .build();
    }
}
