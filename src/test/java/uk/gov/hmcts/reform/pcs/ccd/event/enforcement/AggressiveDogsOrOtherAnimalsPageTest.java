package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AggressiveDogsOrOtherAnimalsPage;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AggressiveDogsOrOtherAnimalsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new AggressiveDogsOrOtherAnimalsPage());
    }

    @ParameterizedTest
    @MethodSource("invalidTextScenarios")
    void shouldRequireTextWhenInvalid(String invalidText) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementDogsOrOtherAnimalsDetails(invalidText).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter details");
    }

    @ParameterizedTest
    @MethodSource("validTextScenarios")
    void shouldAcceptValidText(String text) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementDogsOrOtherAnimalsDetails(text).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementDogsOrOtherAnimalsDetails()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);

        PCSCase caseData = PCSCase.builder()
            .enforcementDogsOrOtherAnimalsDetails(longText).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("In 'What kind of animals do they have?', " +
                                                             "you have entered more than the maximum number of characters (6800)");
    }


    @Test
    void shouldPreserveDataWhenValid() {
        // Given
        String validText = "The defendant has a history of criminal and antisocial behaviour";
        PCSCase caseData = PCSCase.builder()
            .enforcementDogsOrOtherAnimalsDetails("Some dangerous animals")
            .enforcementViolentDetails("Some violent text")
            .enforcementFirearmsDetails("Some firearms text")
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementDogsOrOtherAnimalsDetails())
            .isEqualTo("Some dangerous animals");
        assertThat(response.getData().getEnforcementViolentDetails())
            .isEqualTo("Some violent text");
        assertThat(response.getData().getEnforcementFirearmsDetails())
            .isEqualTo("Some firearms text");
    }

    private static Stream<String> validTextScenarios() {
        return Stream.of(
            "Short text",
            "The defendant has a history of having british bulldogs",
            "A".repeat(1),
            "A".repeat(6800)
        );
    }

    private static Stream<String> invalidTextScenarios() {
        return Stream.of(
            " ",
            ""
        );
    }
}

