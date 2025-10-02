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
    class ValidDescriptionTests {

        @Test
        void shouldPassWithNullAdditionalDocuments() {
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(null)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithEmptyAdditionalDocuments() {
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(new ArrayList<>())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithNullDocumentObjects() {
            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(null).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithNullDescriptions() {
            AdditionalDocument document = AdditionalDocument.builder()
                .description(null)
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithEmptyDescriptions() {
            AdditionalDocument document = AdditionalDocument.builder()
                .description("")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithShortDescriptions() {
            AdditionalDocument document = AdditionalDocument.builder()
                .description("Short description")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithExactly62Characters() {
            String description62Chars = "A".repeat(62);
            
            AdditionalDocument document = AdditionalDocument.builder()
                .description(description62Chars)
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }
    }

    @Nested
    class InvalidDescriptionTests {

        @Test
        void shouldFailWithDescriptionOver62Characters() {
            String longDescription = "A".repeat(63);
            
            AdditionalDocument document = AdditionalDocument.builder()
                .description(longDescription)
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }

        @Test
        void shouldFailWithVeryLongDescription() {
            String veryLongDescription = "A".repeat(100);
            
            AdditionalDocument document = AdditionalDocument.builder()
                .description(veryLongDescription)
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }
    }

    @Nested
    class MultipleDocumentsTests {

        @Test
        void shouldPassWithAllValidDocuments() {
            AdditionalDocument document1 = AdditionalDocument.builder()
                .description("Short description 1")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            AdditionalDocument document2 = AdditionalDocument.builder()
                .description("Short description 2")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document1).build());
            documents.add(ListValue.<AdditionalDocument>builder().value(document2).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldFailWithMixedValidAndInvalidDocuments() {
            AdditionalDocument validDocument = AdditionalDocument.builder()
                .description("Short description")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            AdditionalDocument invalidDocument = AdditionalDocument.builder()
                .description("A".repeat(63))
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(validDocument).build());
            documents.add(ListValue.<AdditionalDocument>builder().value(invalidDocument).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }

        @Test
        void shouldFailWithMultipleInvalidDocuments() {
            AdditionalDocument invalidDocument1 = AdditionalDocument.builder()
                .description("A".repeat(63))
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            AdditionalDocument invalidDocument2 = AdditionalDocument.builder()
                .description("B".repeat(100))
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(invalidDocument1).build());
            documents.add(ListValue.<AdditionalDocument>builder().value(invalidDocument2).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldPassWithWhitespaceOnlyDescription() {
            AdditionalDocument document = AdditionalDocument.builder()
                .description("   ")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldPassWithSpecialCharacters() {
            AdditionalDocument document = AdditionalDocument.builder()
                .description("Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?")
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNullOrEmpty();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldFailWithSpecialCharactersOver62Chars() {
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?".repeat(3); // 60 chars, but we'll make it 63
            
            AdditionalDocument document = AdditionalDocument.builder()
                .description(specialChars + "ABC") // 63 chars total
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .build();

            List<ListValue<AdditionalDocument>> documents = new ArrayList<>();
            documents.add(ListValue.<AdditionalDocument>builder().value(document).build());
            
            PCSCase caseData = PCSCase.builder()
                .additionalDocuments(documents)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("The explanation must be 62 characters or fewer");
        }
    }
}
