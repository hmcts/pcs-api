package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.testutil.DocumentTestData;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.ADDITIONAL_DOCUMENT_REQUIRED;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.ALLOWED_FILE_TYPE_GUIDANCE;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.DISALLOWED_FILE_TYPE_ERROR;

@ExtendWith(MockitoExtension.class)
class UploadAdditionalDocumentsDetailsTest extends BasePageTest {


    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new UploadAdditionalDocumentsDetails(
            textAreaValidationService, DocumentTestData.restrictionEnabledFileUploadValidationService()));
    }

    @Test
    void shouldNotReturnErrorsWhenDescriptionIsCorrectLength() {
        // Given
        AdditionalDocument doc = AdditionalDocument.builder()
                .description("Valid description")
                .documentType(selectedDocumentType())
                .document(Document.builder().filename("evidence.pdf").build())
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

    @ParameterizedTest
    @MethodSource("unselectedDocumentTypes")
    void shouldRejectUploadedFileWithoutDocumentType(DynamicList documentType) {
        // Given
        AdditionalDocument doc = AdditionalDocument.builder()
            .description("Valid description")
            .documentType(documentType)
            .document(Document.builder().filename("evidence.pdf").build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().value(doc).build()))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride())
            .isEqualTo(ADDITIONAL_DOCUMENT_REQUIRED);
        assertThat(response.getData().getAdditionalDocuments().getFirst().getValue().getDocument())
            .isEqualTo(doc.getDocument());
    }

    @Test
    void shouldRejectMissingTypeOnSecondUploadedDocument() {
        // Given
        AdditionalDocument validDoc = AdditionalDocument.builder()
            .documentType(selectedDocumentType())
            .document(Document.builder().filename("evidence.pdf").build())
            .build();
        AdditionalDocument missingDocType = AdditionalDocument.builder()
            .document(Document.builder().filename("evidence.pdf").build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder().value(validDoc).build(),
                ListValue.<AdditionalDocument>builder().value(missingDocType).build()))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo(ADDITIONAL_DOCUMENT_REQUIRED);
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

    @Test
    void shouldReturnRequiredErrorWhenNoAdditionalDocumentUploaded() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo(ADDITIONAL_DOCUMENT_REQUIRED);
    }

    @Test
    void shouldReturnRequiredErrorWhenAdditionalDocumentRowHasNoFile() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder()
                 .value(new AdditionalDocument()).build()))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo(ADDITIONAL_DOCUMENT_REQUIRED);
    }

    @Test
    void shouldReturnErrorWhenAdditionalDocumentIsDisallowedFileType() {
        // Given
        AdditionalDocument doc = AdditionalDocument.builder()
                .description("Valid description")
                .documentType(selectedDocumentType())
                .document(Document.builder().filename("evidence.mp4").build())
                .build();

        PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().value(doc).build()))
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride())
            .isEqualTo(DISALLOWED_FILE_TYPE_ERROR + "\n" + ALLOWED_FILE_TYPE_GUIDANCE);
    }

    private static Stream<DynamicList> unselectedDocumentTypes() {
        return Stream.of(
            null,
            new DynamicList(null, List.of(new DynamicListElement(UUID.randomUUID(), "Witness statement"))));
    }

    private static DynamicList selectedDocumentType() {
        return new DynamicList(new DynamicListElement(UUID.randomUUID(), "Witness statement"), List.of());
    }

}
