package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RiskCategoryTest {

    @ParameterizedTest
    @MethodSource("enumsWithLabels")
    void shouldReturnCorrectLabel(RiskCategory category, String expectedLabel) {
        assertThat(category.getLabel()).isEqualTo(expectedLabel);
    }

    @Test
    void shouldImplementHasLabelInterface() {
        // Verify that RiskCategory implements HasLabel
        assertThat(RiskCategory.VIOLENT_OR_AGGRESSIVE).isInstanceOf(uk.gov.hmcts.ccd.sdk.api.HasLabel.class);
    }

    @Test
    void shouldHaveAllSevenRiskCategories() {
        RiskCategory[] values = RiskCategory.values();
        assertThat(values).hasSize(7);
    }

    @Test
    void shouldHaveExpectedEnumValues() {
        assertThat(RiskCategory.VIOLENT_OR_AGGRESSIVE).isNotNull();
        assertThat(RiskCategory.FIREARMS_POSSESSION).isNotNull();
        assertThat(RiskCategory.CRIMINAL_OR_ANTISOCIAL).isNotNull();
        assertThat(RiskCategory.VERBAL_OR_WRITTEN_THREATS).isNotNull();
        assertThat(RiskCategory.PROTEST_GROUP_MEMBER).isNotNull();
        assertThat(RiskCategory.AGENCY_VISITS).isNotNull();
        assertThat(RiskCategory.AGGRESSIVE_ANIMALS).isNotNull();
    }

    private static Stream<Arguments> enumsWithLabels() {
        return Stream.of(
            arguments(RiskCategory.VIOLENT_OR_AGGRESSIVE, "Violent or aggressive behaviour"),
            arguments(RiskCategory.FIREARMS_POSSESSION, "History of firearm possession"),
            arguments(RiskCategory.CRIMINAL_OR_ANTISOCIAL, "Criminal or antisocial behaviour"),
            arguments(RiskCategory.VERBAL_OR_WRITTEN_THREATS, "Verbal or written threats"),
            arguments(RiskCategory.PROTEST_GROUP_MEMBER, "Member of a group that protests evictions"),
            arguments(RiskCategory.AGENCY_VISITS, "Police or social services visits to the property"),
            arguments(RiskCategory.AGGRESSIVE_ANIMALS, "Aggressive dogs or other animals")
        );
    }
}
