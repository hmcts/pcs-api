package uk.gov.hmcts.reform.pcs.ccd.page.addReviewDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddCaseReviewDatePageTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), any());
        setPageUnderTest(new AddCaseReviewDatePage(textAreaValidationService));
    }

    @Test
    void shouldValidateReviewDateDescriptions() {
        // Given
        String description1 = "description1";
        ListValue<ReviewDate> reviewDate1 = ListValue.<ReviewDate>builder()
            .value(
                ReviewDate.builder()
                    .description(description1)
                    .build()
            ).build();

        String description2 = "description2";
        ListValue<ReviewDate> reviewDate2 = ListValue.<ReviewDate>builder()
            .value(
                ReviewDate.builder()
                    .description(description2)
                    .build()
            ).build();

        PCSCase caseData = PCSCase.builder()
            .reviewDates(List.of(reviewDate1, reviewDate2))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();

        verify(textAreaValidationService).validateMultipleTextAreas(
            argThat(f -> f.fieldValue.equals(description1)
                && f.fieldLabel.equals("Description of review")
                && f.maxCharacters == 500),

            argThat(f -> f.fieldValue.equals(description2)
                && f.fieldLabel.equals("Description of review")
                && f.maxCharacters == 500)
        );
    }
}
