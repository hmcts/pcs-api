package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateSingleTextArea(any(), any(), anyInt());
        lenient().doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), any());

        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds(textAreaValidationService));
    }

    @Test
    void shouldErrorWhenNoAdditionalGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(Set.of())
                    .additionalOtherGround(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo("Please select at least one ground");
    }

    @Test
    void shouldNotErrorWhenAdditionalMandatoryGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(
                        Set.of(AssuredAdditionalMandatoryGrounds.REDEVELOPMENT_GROUND6)
                    )
                    .additionalDiscretionaryGrounds(Set.of())
                    .additionalOtherGround(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldNotErrorWhenAdditionalDiscretionaryGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(
                            Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12))
                    .additionalOtherGround(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should validate otherGroundDescription when provided")
    void shouldValidateOtherGroundDescriptionWhenProvided() {
        // Given
        AssuredRentArrearsPossessionGrounds assuredRentArrearsPossessionGrounds =
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(Set.of())
                    .additionalOtherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                    .additionalOtherGroundDescription("Valid ground description")
                    .build();

        PCSCase caseData = PCSCase.builder()
                .assuredRentArrearsPossessionGrounds(assuredRentArrearsPossessionGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null otherGroundDescription gracefully")
    void shouldHandleNullOtherGroundDescriptionGracefully() {
        // Given
        AssuredRentArrearsPossessionGrounds assuredRentArrearsPossessionGrounds =
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(Set.of())
                    .additionalOtherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                    .additionalOtherGroundDescription(null)
                    .build();

        PCSCase caseData = PCSCase.builder()
                .assuredRentArrearsPossessionGrounds(assuredRentArrearsPossessionGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should return validation errors when otherGroundDescription exceeds limit")
    void shouldReturnValidationErrorsWhenOtherGroundDescriptionExceedsLimit() {
        // Given
        String longText = "a".repeat(501); // Exceeds MEDIUM_TEXT_LIMIT (500)
        List<String> validationErrors = List.of("Error message");

        lenient().doReturn(validationErrors).when(textAreaValidationService)
                .validateSingleTextArea(any(), any(), anyInt());

        AssuredRentArrearsPossessionGrounds assuredRentArrearsPossessionGrounds =
                AssuredRentArrearsPossessionGrounds.builder()
                        .additionalOtherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                        .additionalOtherGroundDescription(longText)
                        .build();

        PCSCase caseData = PCSCase.builder()
                .assuredRentArrearsPossessionGrounds(assuredRentArrearsPossessionGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).isNotEmpty();
    }
}
