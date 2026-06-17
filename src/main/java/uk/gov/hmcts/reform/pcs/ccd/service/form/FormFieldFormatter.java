package uk.gov.hmcts.reform.pcs.ccd.service.form;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Field-level formatting shared by the claim-form and defence-form payload builders, so money, date
 * and address rendering have a single definition. The form-specific formatters delegate the common
 * helpers here and keep only their own type overloads.
 */
public final class FormFieldFormatter {

    private FormFieldFormatter() {
    }

    private static final DateTimeFormatter LONG_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    public static boolean isPopulated(String text) {
        return text != null && !text.isBlank();
    }

    public static boolean isYes(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.YES;
    }

    public static boolean isYes(YesNoNotSure yesNo) {
        return yesNo == YesNoNotSure.YES;
    }

    public static boolean isNo(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.NO;
    }

    public static boolean isNo(YesNoNotSure yesNo) {
        return yesNo == YesNoNotSure.NO;
    }

    public static String toLabel(VerticalYesNo yesNo) {
        return yesNo == null ? null : yesNo.getLabel();
    }

    public static String toLabel(YesNoNotSure yesNo) {
        return yesNo == null ? null : yesNo.getLabel();
    }

    // GBP currency string from a pounds amount, e.g. £1,200.00. Null-safe, no division.
    public static String formatGbp(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return NumberFormat.getCurrencyInstance(Locale.UK).format(amount);
    }

    // Long date, e.g. "10 January 2024". Null-safe.
    public static String formatLongDate(LocalDate date) {
        return date == null ? null : date.format(LONG_DATE_FORMAT);
    }

    public static ClaimFormAddress toFormAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return ClaimFormAddress.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postcode(address.getPostcode())
            .country(address.getCountry())
            .build();
    }

    public static ClaimFormAddress toFormAddress(AddressUK address) {
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
