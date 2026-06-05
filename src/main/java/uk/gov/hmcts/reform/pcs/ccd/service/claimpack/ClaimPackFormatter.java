package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PossessionGroundLabelResolver;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Stateless value formatters and predicates shared across the claim-pack mapping — keeps
 * {@link ClaimPackPayloadBuilder} focused on orchestration rather than string-fiddling.
 */
final class ClaimPackFormatter {

    private ClaimPackFormatter() {
    }

    static boolean isPopulated(String text) {
        return text != null && !text.isBlank();
    }

    static boolean isYes(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.YES;
    }

    static boolean isNo(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.NO;
    }

    // Null-safe title-case label for direct rendering — VerticalYesNo.YES → "Yes".
    static String toLabel(VerticalYesNo yesNo) {
        return yesNo == null ? null : yesNo.getLabel();
    }

    static String toLabel(NoticeServiceMethod method) {
        return method == null ? null : method.getLabel();
    }

    // Bridge from the CCD SDK YesOrNo (some entities) to the payload's VerticalYesNo; null-safe.
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

    static String formatGroundLabel(ClaimGroundEntity ground) {
        return PossessionGroundLabelResolver.label(ground.getCategory(), ground.getCode());
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

    // AC06: first party "Defendant 1 details", subsequent "Additional defendant N details".
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

    // CombinedLicenceType carries no label of its own — reuse the England/Wales source enums that do.
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
