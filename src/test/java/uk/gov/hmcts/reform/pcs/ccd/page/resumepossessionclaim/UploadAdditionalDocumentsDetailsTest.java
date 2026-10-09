package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.ccd.testutil.DocumentTestData;

import java.util.List;

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
    void shouldNotReturnErrorsWhenEnglandDescriptionIsCorrectLength() {
        AdditionalDocumentEngland additionalDocument = AdditionalDocumentEngland.builder()
            .documentType(AdditionalDocumentTypeEngland.WITNESS_STATEMENT)
            .description("Valid description")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .additionalDocumentsEngland(List.of(
                ListValue.<AdditionalDocumentEngland>builder()
                    .value(additionalDocument)
                    .build()))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnValidationErrorsWhenEnglandDescriptionTooLong() {
        AdditionalDocumentEngland additionalDocument = AdditionalDocumentEngland.builder()
            .documentType(AdditionalDocumentTypeEngland.WITNESS_STATEMENT)
            .description("a".repeat(61))
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .additionalDocumentsEngland(List.of(
                ListValue.<AdditionalDocumentEngland>builder()
                    .value(additionalDocument)
                    .build()))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride())
            .isNotNull()
            .contains("more than the maximum number of characters");
    }

    @Test

    void shouldKeepEnglandDocumentTypeOnSubmit() {
        AdditionalDocumentEngland additionalDocument = AdditionalDocumentEngland.builder()
            .documentType(AdditionalDocumentTypeEngland.TENANCY_AGREEMENT)
            .description("Valid description")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .additionalDocumentsEngland(List.of(
                ListValue.<AdditionalDocumentEngland>builder()
                    .value(additionalDocument)
                    .build()))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        AdditionalDocumentEngland returnedDocument =
            response.getData().getAdditionalDocumentsEngland().getFirst().getValue();
        assertThat(returnedDocument.getDocumentType()).isEqualTo(AdditionalDocumentTypeEngland.TENANCY_AGREEMENT);
    }

    @Test
    void shouldNotReturnErrorsWhenWalesDescriptionIsCorrectLength() {
        AdditionalDocumentWales additionalDocument = AdditionalDocumentWales.builder()
            .documentType(AdditionalDocumentTypeWales.WITNESS_STATEMENT)
            .description("Valid description")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .additionalDocumentsWales(List.of(
                ListValue.<AdditionalDocumentWales>builder()
                    .value(additionalDocument)
                    .build()))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnValidationErrorsWhenWalesDescriptionTooLong() {
        AdditionalDocumentWales additionalDocument = AdditionalDocumentWales.builder()
            .documentType(AdditionalDocumentTypeWales.WITNESS_STATEMENT)
            .description("a".repeat(61))
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .additionalDocumentsWales(List.of(
                ListValue.<AdditionalDocumentWales>builder()
                    .value(additionalDocument)
                    .build()))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride())
            .isNotNull()
            .contains("more than the maximum number of characters");
    }

    @Test
    void shouldKeepWalesDocumentTypeOnSubmit() {
        AdditionalDocumentWales additionalDocument = AdditionalDocumentWales.builder()
            .documentType(AdditionalDocumentTypeWales.OCCUPATION_LICENCE)
            .description("Valid description")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .additionalDocumentsWales(List.of(
                ListValue.<AdditionalDocumentWales>builder()
                    .value(additionalDocument)
                    .build()))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        AdditionalDocumentWales returnedDocument =
            response.getData().getAdditionalDocumentsWales().getFirst().getValue();
        assertThat(returnedDocument.getDocumentType()).isEqualTo(AdditionalDocumentTypeWales.OCCUPATION_LICENCE);
    }

    void shouldReturnRequiredErrorWhenNoAdditionalDocumentUploaded() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

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


}
