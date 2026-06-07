package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;

@Builder
@Data
public class ClaimGroundSummary {

    private ClaimGroundCategory category;

    private String code;

    private String label;

    private String description;

    private String reason;

    private YesOrNo isRentArrears;

    @JsonIgnore
    private int categoryRank;

    @JsonIgnore
    private int groundRank;

    /**
     * Resolve a persisted ground ({@link ClaimGroundCategory} + code) to its ground enum.
     */
    public static PossessionGroundEnum resolveGround(ClaimGroundCategory category, String code) {
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

    /**
     * Human-readable label for a persisted ground, falling back to the raw code if it can't be
     * resolved (so a malformed code never breaks rendering).
     */
    public static String labelFor(ClaimGroundCategory category, String code) {
        if (category == null || code == null) {
            return code;
        }
        try {
            return resolveGround(category, code).getLabel();
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

}
