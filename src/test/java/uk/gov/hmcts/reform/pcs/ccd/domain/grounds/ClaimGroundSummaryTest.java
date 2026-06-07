package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimGroundSummaryTest {

    @ParameterizedTest
    @MethodSource("allGroundCodes")
    void resolvesEveryGroundCodeToItsHumanLabel(ClaimGroundCategory category, String code, String expectedLabel) {
        assertThat(ClaimGroundSummary.labelFor(category, code)).isEqualTo(expectedLabel);
    }

    @Test
    void returnsRawCodeWhenCategoryNull() {
        assertThat(ClaimGroundSummary.labelFor(null, "SERIOUS_RENT_ARREARS_GROUND8"))
            .isEqualTo("SERIOUS_RENT_ARREARS_GROUND8");
    }

    @Test
    void returnsNullWhenCodeNull() {
        assertThat(ClaimGroundSummary.labelFor(ClaimGroundCategory.ASSURED_MANDATORY, null)).isNull();
    }

    @Test
    void fallsBackToRawCodeWhenCodeUnknownForCategory() {
        assertThat(ClaimGroundSummary.labelFor(ClaimGroundCategory.ASSURED_MANDATORY, "NOT_A_REAL_CODE"))
            .isEqualTo("NOT_A_REAL_CODE");
    }

    private static Stream<Arguments> allGroundCodes() {
        return Stream.of(
            casesFor(ClaimGroundCategory.ASSURED_MANDATORY, AssuredMandatoryGround.class),
            casesFor(ClaimGroundCategory.ASSURED_DISCRETIONARY, AssuredDiscretionaryGround.class),
            casesFor(ClaimGroundCategory.ASSURED_OTHER, AssuredAdditionalOtherGround.class),
            casesFor(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY, SecureOrFlexibleMandatoryGrounds.class),
            casesFor(ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL, SecureAntisocialAdditionalGrounds.class),
            casesFor(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT,
                SecureOrFlexibleMandatoryGroundsAlternativeAccomm.class),
            casesFor(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY, SecureOrFlexibleDiscretionaryGrounds.class),
            casesFor(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT,
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.class),
            casesFor(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER, IntroductoryDemotedOrOtherGrounds.class),
            casesFor(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS,
                IntroductoryDemotedOrOtherNoGrounds.class),
            casesFor(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY, MandatoryGroundWales.class),
            casesFor(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY, DiscretionaryGroundWales.class),
            casesFor(ClaimGroundCategory.WALES_SECURE_MANDATORY, SecureContractMandatoryGroundsWales.class),
            casesFor(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY, SecureContractDiscretionaryGroundsWales.class),
            casesFor(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT, EstateManagementGroundsWales.class),
            casesFor(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT, EstateManagementGroundsWales.class)
        ).flatMap(s -> s);
    }

    private static <E extends Enum<E> & PossessionGroundEnum> Stream<Arguments> casesFor(
        ClaimGroundCategory category, Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
            .map(ground -> Arguments.argumentSet(
                category.name() + " / " + ground.name(),
                category, ground.name(), ground.getLabel()));
    }
}
