package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnforcementRiskValidationUtilsTest {

    @Test
    void shouldReturnCorrectCharacterLimit() {
        assertThat(EnforcementRiskValidationUtils.getCharacterLimit()).isEqualTo(6800);
    }

    @ParameterizedTest
    @MethodSource("provideRiskCategoryErrorMessages")
    void shouldGenerateCorrectErrorMessageForRiskCategory(RiskCategory riskCategory, String expectedErrorMessage) {
        String errorMessage = EnforcementRiskValidationUtils.getCharacterLimitErrorMessage(riskCategory);
        
        assertThat(errorMessage).isEqualTo(expectedErrorMessage);
    }

    @ParameterizedTest
    @NullSource
    void shouldHandleNullRiskCategory(RiskCategory riskCategory) {
        assertThatThrownBy(() -> EnforcementRiskValidationUtils.getCharacterLimitErrorMessage(riskCategory))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldHandleUnknownRiskCategory() {
        // This test assumes there might be enum values not handled in the switch statement
        // If all enum values are handled, this test might need to be adjusted based on actual implementation
        RiskCategory[] allCategories = RiskCategory.values();
        
        for (RiskCategory category : allCategories) {
            // Verify that no category throws an unexpected exception
            String errorMessage = EnforcementRiskValidationUtils.getCharacterLimitErrorMessage(category);
            assertThat(errorMessage)
                    .isNotNull()
                    .isNotEmpty()
                    .contains("you have entered more than the maximum number of characters (6800)");
        }
    }

    private static Stream<Arguments> provideRiskCategoryErrorMessages() {
        return Stream.of(
                Arguments.of(
                        RiskCategory.VIOLENT_OR_AGGRESSIVE,
                        "In 'How have they been violent or aggressive?', you have entered more than the "
                                + "maximum number of characters (6800)"
                ),
                Arguments.of(
                        RiskCategory.FIREARMS_POSSESSION,
                        "In 'What is their history of firearm possession?', you have entered more than the "
                                + "maximum number of characters (6800)"
                ),
                Arguments.of(
                        RiskCategory.CRIMINAL_OR_ANTISOCIAL,
                        "In 'What is their history of criminal or antisocial behaviour?', you have entered more "
                                + "than the maximum number of characters (6800)"
                ),
                Arguments.of(
                        RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                        "In 'What verbal or written threats have they made?', you have entered more than the "
                                + "maximum number of characters (6800)"
                ),
                Arguments.of(
                        RiskCategory.PROTEST_GROUP_MEMBER,
                        "In 'What group do they belong to that protests evictions?', you have entered more than the "
                                + "maximum number of characters (6800)"
                ),
                Arguments.of(
                        RiskCategory.AGENCY_VISITS,
                        "In 'What visits from police or social services have there been?', you have entered more "
                                + "than the maximum number of characters (6800)"
                ),
                Arguments.of(
                        RiskCategory.AGGRESSIVE_ANIMALS,
                        "In 'What aggressive dogs or other animals do they have?', you have entered more than the "
                                + "maximum number of characters (6800)"
                )
        );
    }
}
