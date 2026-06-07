package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackAddress;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Value formatting helpers for the claim-pack payload mapping, kept out of
 * {@link ClaimPackPayloadBuilder}.
 */
final class ClaimPackFormatter {

    private ClaimPackFormatter() {
    }

    // Formats: "10 January 2024" and "2:30pm".
    private static final DateTimeFormatter LONG_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    private static final DateTimeFormatter NOTICE_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mma", Locale.UK);

    static boolean isPopulated(String text) {
        return text != null && !text.isBlank();
    }

    static boolean isYes(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.YES;
    }

    static boolean isNo(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.NO;
    }

    // VerticalYesNo to its display label ("Yes"/"No"), null-safe.
    static String toLabel(VerticalYesNo yesNo) {
        return yesNo == null ? null : yesNo.getLabel();
    }

    static String toLabel(NoticeServiceMethod method) {
        return method == null ? null : method.getLabel();
    }

    // Converts the CCD SDK YesOrNo to the payload's VerticalYesNo, null-safe.
    static VerticalYesNo yesOrNoToVertical(YesOrNo yesOrNo) {
        if (yesOrNo == null) {
            return null;
        }
        return yesOrNo == YesOrNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

    // GBP currency string, e.g. £1,200.00. Null-safe.
    static String formatGbp(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return NumberFormat.getCurrencyInstance(Locale.UK).format(amount);
    }

    // Long date, e.g. "10 January 2024". Null-safe.
    static String formatLongDate(LocalDate date) {
        return date == null ? null : date.format(LONG_DATE_FORMAT);
    }

    // Served-notice time, e.g. "2:30pm". Null-safe.
    static String formatNoticeTime(LocalTime time) {
        return time == null ? null : time.format(NOTICE_TIME_FORMAT).toLowerCase(Locale.UK);
    }

    static String formatGroundLabel(ClaimGroundEntity ground) {
        return ClaimGroundSummary.labelFor(ground.getCategory(), ground.getCode());
    }

    static ClaimPackAddress toClaimPackAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return ClaimPackAddress.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postcode(address.getPostcode())
            .country(address.getCountry())
            .build();
    }

    static String formatTenancyLabel(TenancyLicenceEntity tenancy) {
        CombinedLicenceType type = tenancy.getType();
        if (type == null) {
            return null;
        }
        String label = combinedLicenceLabel(type);
        return type == CombinedLicenceType.OTHER && tenancy.getOtherTypeDetails() != null
            ? label + ": " + tenancy.getOtherTypeDetails()
            : label;
    }

    static String formatRentDescription(TenancyLicenceEntity tenancy) {
        if (tenancy.getRentAmount() == null || tenancy.getRentFrequency() == null) {
            return null;
        }
        return formatGbp(tenancy.getRentAmount()) + " (" + tenancy.getRentFrequency().getLabel() + ")";
    }

    // "Defendant 1 details", then "Additional defendant N details" for later defendants.
    static String formatDefendantHeading(int defendantNumber) {
        return defendantNumber == 1
            ? "Defendant 1 details"
            : "Additional defendant " + (defendantNumber - 1) + " details";
    }

    static String formatUnderlesseeHeading(int underlesseeNumber) {
        return underlesseeNumber == 1
            ? "Underlessee or mortgagee 1 details"
            : "Additional underlessee or mortgagee " + (underlesseeNumber - 1) + " details";
    }

    // CombinedLicenceType has no label of its own; look it up from the England or Wales source enum.
    private static String combinedLicenceLabel(CombinedLicenceType type) {
        for (TenancyLicenceType englandType : TenancyLicenceType.values()) {
            if (englandType.getCombinedLicenceType() == type) {
                return englandType.getLabel();
            }
        }
        for (OccupationLicenceTypeWales walesType : OccupationLicenceTypeWales.values()) {
            if (walesType.getCombinedLicenceType() == type) {
                return walesType.getLabel();
            }
        }
        return type.name();
    }
}
