package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppType;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.MEDIUM_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class ApplicationDetailsTest extends BasePageTest {

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2025, 8, 27);

    @Mock
    private Clock ukClock;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new ApplicationDetails(ukClock, textAreaValidationService));
    }

    @ParameterizedTest
    @MethodSource("dateReceivedScenarios")
    void shouldValidateDateReceivedIsInThePast(LocalDate dateReceived, boolean isValid) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                .applicationTypeOption(EnterGenAppType.ADJOURN)
                .dateReceived(dateReceived)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (isValid) {
            assertThat(response.getErrorMessageOverride()).isNull();
        } else {
            assertThat(response.getErrorMessageOverride())
                .isEqualTo("Date the application was received must be in the past");
        }
    }

    private static Stream<Arguments> dateReceivedScenarios() {
        return Stream.of(
            arguments(FIXED_CURRENT_DATE.plusDays(1), false),
            arguments(FIXED_CURRENT_DATE, false),
            arguments(FIXED_CURRENT_DATE.minusDays(1), true),
            arguments(FIXED_CURRENT_DATE.minusYears(5), true)
        );
    }

    @Test
    void shouldAcceptValidSomethingElseDetails() {
        // Given
        String details = "some categories";
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                .applicationTypeOption(EnterGenAppType.SOMETHING_ELSE)
                .somethingElseDetails(details)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldRejectSomethingElseDetailsOverCharacterLimit() {
        // Given
        String longText = "a".repeat(MEDIUM_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                .applicationTypeOption(EnterGenAppType.SOMETHING_ELSE)
                .somethingElseDetails(longText)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(
            CHARACTER_LIMIT_ERROR_TEMPLATE,
            "Which categories apply",
            "500"
        );
        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);
    }

    @Test
    void shouldNotValidateSomethingElseDetailsForOtherApplicationTypes() {
        // Given
        String longText = "a".repeat(MEDIUM_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                .applicationTypeOption(EnterGenAppType.ADJOURN)
                .somethingElseDetails(longText)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
    }

}
