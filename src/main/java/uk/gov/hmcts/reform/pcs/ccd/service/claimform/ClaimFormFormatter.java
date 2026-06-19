package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Value formatting helpers specific to the claim-form payload mapping, kept out of
 * {@link ClaimFormPayloadBuilder}. Shared money/date/address/yes-no helpers delegate to
 * {@link FormFieldFormatter}; only the claim-specific formatters live here.
 */
final class ClaimFormFormatter {

    private ClaimFormFormatter() {
    }

    // Served-notice time format, e.g. "2:30pm".
    private static final DateTimeFormatter NOTICE_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mma", Locale.UK);

    static boolean isPopulated(String text) {
        return FormFieldFormatter.isPopulated(text);
    }

    static boolean isYes(VerticalYesNo yesNo) {
        return FormFieldFormatter.isYes(yesNo);
    }

    static boolean isNo(VerticalYesNo yesNo) {
        return FormFieldFormatter.isNo(yesNo);
    }

    static String toLabel(VerticalYesNo yesNo) {
        return FormFieldFormatter.toLabel(yesNo);
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

    static String formatGbp(BigDecimal amount) {
        return FormFieldFormatter.formatGbp(amount);
    }

    static String formatLongDate(LocalDate date) {
        return FormFieldFormatter.formatLongDate(date);
    }

    // Served-notice time, e.g. "2:30pm". Null-safe.
    static String formatNoticeTime(LocalTime time) {
        return time == null ? null : time.format(NOTICE_TIME_FORMAT).toLowerCase(Locale.UK);
    }

    static String formatGroundLabel(ClaimGroundEntity ground) {
        return ClaimGroundSummary.labelFor(ground.getCategory(), ground.getCode());
    }

    // Antisocial behaviour is a parent checkbox whose children are the s.84A conditions; the form
    // names each "Antisocial behaviour: Condition N of Section 84A of the Housing Act 1985".
    static String formatAntisocialGroundLabel(ClaimGroundEntity ground) {
        return SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.getLabel() + ": " + formatGroundLabel(ground);
    }

    // Ground 1 is a parent checkbox whose children are "Rent arrears" / "Breach of the tenancy"; the
    // form names each "Rent arrears or breach of the tenancy (ground 1): <child>".
    static String formatRentArrearsOrBreachLabel(ClaimGroundEntity ground, RentArrearsOrBreachOfTenancy child) {
        return formatGroundLabel(ground) + ": " + child.getLabel();
    }

    static ClaimFormAddress toClaimFormAddress(AddressEntity address) {
        return FormFieldFormatter.toFormAddress(address);
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

    static String formatRentFrequency(TenancyLicenceEntity tenancy) {
        return tenancy.getRentFrequency() == null ? null : tenancy.getRentFrequency().getLabel();
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
