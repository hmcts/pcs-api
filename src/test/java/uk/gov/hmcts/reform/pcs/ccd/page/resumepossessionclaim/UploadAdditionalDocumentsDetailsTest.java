package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UploadAdditionalDocumentsDetailsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new UploadAdditionalDocumentsDetails());
    }

    @Nested
    class MidEventValidationTests {

        @Test
        void shouldPassValidationWhenNoAdditionalDocuments() {
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(null)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassValidationWhenAdditionalDocumentsListIsEmpty() {
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(new ArrayList<>())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassValidationWhenDescriptionIsWithinLimit() {
            AdditionalDocument document = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description("Short description within 62 characters")
                .build();

            ListValue<AdditionalDocument> listValue = ListValue.<AdditionalDocument>builder()
                .value(document)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassValidationWhenDescriptionIsExactly62Characters() {
            String description = "A".repeat(62);
            
            AdditionalDocument document = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.RENT_STATEMENT)
                .description(description)
                .build();

            ListValue<AdditionalDocument> listValue = ListValue.<AdditionalDocument>builder()
                .value(document)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldFailValidationWhenDescriptionExceeds62Characters() {
            String description = "A".repeat(63);
            
            AdditionalDocument document = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.TENANCY_AGREEMENT)
                .description(description)
                .build();

            ListValue<AdditionalDocument> listValue = ListValue.<AdditionalDocument>builder()
                .value(document)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).hasSize(1);
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }

        @Test
        void shouldFailValidationWhenMultipleDocumentsHaveLongDescriptions() {
            String longDescription = "A".repeat(100);
            
            AdditionalDocument document1 = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description(longDescription)
                .build();

            AdditionalDocument document2 = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.RENT_STATEMENT)
                .description(longDescription)
                .build();

            ListValue<AdditionalDocument> listValue1 = ListValue.<AdditionalDocument>builder()
                .value(document1)
                .build();

            ListValue<AdditionalDocument> listValue2 = ListValue.<AdditionalDocument>builder()
                .value(document2)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue1, listValue2))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getErrors()).containsOnly("The explanation must be 62 characters or fewer");
        }

        @Test
        void shouldPassValidationWhenSomeDocumentsAreValidAndSomeHaveNullDescriptions() {
            AdditionalDocument validDocument = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description("Valid short description")
                .build();

            AdditionalDocument documentWithNullDescription = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.RENT_STATEMENT)
                .description(null)
                .build();

            ListValue<AdditionalDocument> listValue1 = ListValue.<AdditionalDocument>builder()
                .value(validDocument)
                .build();

            ListValue<AdditionalDocument> listValue2 = ListValue.<AdditionalDocument>builder()
                .value(documentWithNullDescription)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue1, listValue2))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassValidationWhenSomeDocumentsAreValidAndSomeHaveEmptyDescriptions() {
            AdditionalDocument validDocument = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description("Valid short description")
                .build();

            AdditionalDocument documentWithEmptyDescription = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.RENT_STATEMENT)
                .description("")
                .build();

            ListValue<AdditionalDocument> listValue1 = ListValue.<AdditionalDocument>builder()
                .value(validDocument)
                .build();

            ListValue<AdditionalDocument> listValue2 = ListValue.<AdditionalDocument>builder()
                .value(documentWithEmptyDescription)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue1, listValue2))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldHandleMixedValidAndInvalidDocuments() {
            AdditionalDocument validDocument = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description("Valid short description")
                .build();

            String longDescription = "A".repeat(100);
            AdditionalDocument invalidDocument = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.RENT_STATEMENT)
                .description(longDescription)
                .build();

            ListValue<AdditionalDocument> listValue1 = ListValue.<AdditionalDocument>builder()
                .value(validDocument)
                .build();

            ListValue<AdditionalDocument> listValue2 = ListValue.<AdditionalDocument>builder()
                .value(invalidDocument)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue1, listValue2))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).hasSize(1);
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldHandleNullDocumentInList() {
            ListValue<AdditionalDocument> listValueWithNullDocument = ListValue.<AdditionalDocument>builder()
                .value(null)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValueWithNullDocument))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldHandleDocumentWithOnlyWhitespaceDescription() {
            AdditionalDocument document = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description("   ")
                .build();

            ListValue<AdditionalDocument> listValue = ListValue.<AdditionalDocument>builder()
                .value(document)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldHandleVeryLongDescription() {
            String veryLongDescription = "A".repeat(1000);
            
            AdditionalDocument document = AdditionalDocument.builder()
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description(veryLongDescription)
                .build();

            ListValue<AdditionalDocument> listValue = ListValue.<AdditionalDocument>builder()
                .value(document)
                .build();

            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(listValue))
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).hasSize(1);
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }
    }
}
