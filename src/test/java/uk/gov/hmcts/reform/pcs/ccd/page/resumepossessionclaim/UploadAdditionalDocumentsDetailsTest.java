package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UploadAdditionalDocumentsDetailsTest extends BasePageTest {


    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new UploadAdditionalDocumentsDetails(textAreaValidationService));
    }

    @Test
    void shouldNotReturnErrorsWhenDescriptionIsCorrectLength() {
        // Given
        AdditionalDocument doc = AdditionalDocument.builder()
            .description("Valid description")
            .build();

        PCSCase caseData = PCSCase.builder()
            .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().value(doc).build()))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnValidationErrorsWhenDescriptionTooLong() {
        // Given
        String longDescription = "a".repeat(61);
        AdditionalDocument doc = AdditionalDocument.builder()
            .description(longDescription)
            .build();

        PCSCase caseData = PCSCase.builder()
            .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().value(doc).build()))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride())
            .isNotNull()
            .contains("more than the maximum number of characters");
    }

}
