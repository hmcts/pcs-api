package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;

/**
 * Resolves a persisted ground ({@link ClaimGroundCategory} + code) to its ground enum and
 * human-readable label. Intended as the single home for the category→enum mapping.
 * NOTE: {@code ClaimGroundsView} still has a private copy of this switch — consolidate it onto
 * this resolver in a follow-up (kept out of this change to stay scoped to the claim pack).
 */
public final class PossessionGroundLabelResolver {

    private PossessionGroundLabelResolver() {
    }

    /**
     * Human-readable label for a persisted ground, falling back to the raw code if it can't be
     * resolved (so a malformed code never breaks rendering).
     */
    public static String label(ClaimGroundCategory category, String code) {
        if (category == null || code == null) {
            return code;
        }
        try {
            return resolve(category, code).getLabel();
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

    public static PossessionGroundEnum resolve(ClaimGroundCategory category, String code) {
        return switch (category) {
            case ASSURED_MANDATORY -> AssuredMandatoryGround.valueOf(code);
            case ASSURED_DISCRETIONARY -> AssuredDiscretionaryGround.valueOf(code);
            case ASSURED_OTHER -> AssuredAdditionalOtherGround.valueOf(code);
            case SECURE_OR_FLEXIBLE_MANDATORY -> SecureOrFlexibleMandatoryGrounds.valueOf(code);
            case SECURE_OR_FLEXIBLE_ANTISOCIAL -> SecureAntisocialAdditionalGrounds.valueOf(code);
            case SECURE_OR_FLEXIBLE_MANDATORY_ALT -> SecureOrFlexibleMandatoryGroundsAlternativeAccomm.valueOf(code);
            case SECURE_OR_FLEXIBLE_DISCRETIONARY -> SecureOrFlexibleDiscretionaryGrounds.valueOf(code);
            case SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT
                -> SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.valueOf(code);
            case INTRODUCTORY_DEMOTED_OTHER -> IntroductoryDemotedOrOtherGrounds.valueOf(code);
            case INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS -> IntroductoryDemotedOrOtherNoGrounds.valueOf(code);
            case WALES_STANDARD_OTHER_MANDATORY -> MandatoryGroundWales.valueOf(code);
            case WALES_STANDARD_OTHER_DISCRETIONARY -> DiscretionaryGroundWales.valueOf(code);
            case WALES_SECURE_MANDATORY -> SecureContractMandatoryGroundsWales.valueOf(code);
            case WALES_SECURE_DISCRETIONARY -> SecureContractDiscretionaryGroundsWales.valueOf(code);
            case WALES_STANDARD_OTHER_ESTATE_MANAGEMENT, WALES_SECURE_ESTATE_MANAGEMENT
                -> EstateManagementGroundsWales.valueOf(code);
        };
    }
}
