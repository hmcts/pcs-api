package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PossessionGroundsValidationUtilTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @InjectMocks
    private PossessionGroundsValidationUtil underTest;

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
        AboutToStartOrSubmitResponse<PCSCase, State> response =
                underTest.validateOtherGroundDescription(caseData, "some description");

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
        AboutToStartOrSubmitResponse<PCSCase, State> response =
                underTest.validateOtherGroundDescription(caseData, null);

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
        AboutToStartOrSubmitResponse<PCSCase, State> response =
                underTest.validateOtherGroundDescription(caseData, longText);

        // Then
        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).isNotEmpty();
    }
}