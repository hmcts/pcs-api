package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.model.PartyDocumentDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenDocumentsServiceTest {

    private CitizenDocumentsService citizenDocumentsService;

    @Mock
    private PCSCase pcsCase;

    @BeforeEach
    void setUp() {
        citizenDocumentsService = new CitizenDocumentsService();
    }

    @Test
    void shouldBuildCitizenDocumentsFromValidData() {
        // Given
        Document document1 = Document.builder()
            .filename("witness_statement.pdf")
            .url("http://example.com/witness.pdf")
            .binaryUrl("http://example.com/witness.pdf/binary")
            .build();

        Document document2 = Document.builder()
            .filename("rent_statement.pdf")
            .url("http://example.com/rent.pdf")
            .binaryUrl("http://example.com/rent.pdf/binary")
            .build();

        AdditionalDocument additionalDocument1 = AdditionalDocument.builder()
            .description("Witness statement from tenant")
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document1)
            .build();

        AdditionalDocument additionalDocument2 = AdditionalDocument.builder()
            .description("Rent statement for January 2024")
            .documentType(AdditionalDocumentType.RENT_STATEMENT)
            .document(document2)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = Arrays.asList(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(additionalDocument1)
                .build(),
            ListValue.<AdditionalDocument>builder()
                .id("2")
                .value(additionalDocument2)
                .build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(2);

        assertThat(result)
            .extracting(PartyDocumentDto::getDescription)
            .containsExactlyInAnyOrder(
                "Witness statement from tenant",
                "Rent statement for January 2024"
            );

        assertThat(result)
            .extracting(PartyDocumentDto::getDocumentType)
            .containsExactlyInAnyOrder(
                AdditionalDocumentType.WITNESS_STATEMENT,
                AdditionalDocumentType.RENT_STATEMENT
            );

        assertThat(result)
            .extracting(PartyDocumentDto::getDocument)
            .extracting(Document::getFilename)
            .containsExactlyInAnyOrder(
                "witness_statement.pdf",
                "rent_statement.pdf"
            );
    }

    @Test
    void shouldHandleEmptyCitizenDocuments() {
        // Given
        when(pcsCase.getCitizenDocuments()).thenReturn(Collections.emptyList());

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleNullCitizenDocuments() {
        // Given
        when(pcsCase.getCitizenDocuments()).thenReturn(null);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFilterOutDocumentsWithNullDocument() {
        // Given
        Document document = Document.builder()
            .filename("valid.pdf")
            .build();

        AdditionalDocument validDocument = AdditionalDocument.builder()
            .description("Valid document")
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document)
            .build();

        AdditionalDocument invalidDocument = AdditionalDocument.builder()
            .description("Invalid document")
            .documentType(AdditionalDocumentType.RENT_STATEMENT)
            .document(null)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = Arrays.asList(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(validDocument)
                .build(),
            ListValue.<AdditionalDocument>builder()
                .id("2")
                .value(invalidDocument)
                .build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isEqualTo("Valid document");
        assertThat(result.getFirst().getDocumentType()).isEqualTo(AdditionalDocumentType.WITNESS_STATEMENT);
        assertThat(result.getFirst().getDocument()).isEqualTo(document);
    }

    @Test
    void shouldFilterOutNullAdditionalDocuments() {
        // Given
        Document document = Document.builder()
            .filename("valid.pdf")
            .build();

        AdditionalDocument validDocument = AdditionalDocument.builder()
            .description("Valid document")
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = Arrays.asList(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(validDocument)
                .build(),
            ListValue.<AdditionalDocument>builder()
                .id("2")
                .value(null)
                .build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isEqualTo("Valid document");
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        // Given
        Document document = Document.builder()
            .filename("test_document.pdf")
            .url("http://example.com/test.pdf")
            .binaryUrl("http://example.com/test.pdf/binary")
            .build();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .description("Test description")
            .documentType(AdditionalDocumentType.TENANCY_AGREEMENT)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = List.of(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(additionalDocument)
                .build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);

        PartyDocumentDto mappedDocument = result.getFirst();
        assertThat(mappedDocument.getDescription()).isEqualTo("Test description");
        assertThat(mappedDocument.getDocumentType()).isEqualTo(AdditionalDocumentType.TENANCY_AGREEMENT);
        assertThat(mappedDocument.getDocument()).isEqualTo(document);
        assertThat(mappedDocument.getDocument().getFilename()).isEqualTo("test_document.pdf");
        assertThat(mappedDocument.getDocument().getUrl()).isEqualTo("http://example.com/test.pdf");
        assertThat(mappedDocument.getDocument().getBinaryUrl()).isEqualTo("http://example.com/test.pdf/binary");
    }

    @Test
    void shouldHandleNullDescription() {
        // Given
        Document document = Document.builder()
            .filename("test.pdf")
            .build();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .description(null)
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = List.of(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(additionalDocument)
                .build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isNull();
        assertThat(result.getFirst().getDocumentType()).isEqualTo(AdditionalDocumentType.WITNESS_STATEMENT);
        assertThat(result.getFirst().getDocument()).isEqualTo(document);
    }

    @Test
    void shouldHandleNullDocumentType() {
        // Given
        Document document = Document.builder()
            .filename("test.pdf")
            .build();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .description("Document without type")
            .documentType(null)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = List.of(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(additionalDocument)
                .build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isEqualTo("Document without type");
        assertThat(result.getFirst().getDocumentType()).isNull();
        assertThat(result.getFirst().getDocument()).isEqualTo(document);
    }

    @Test
    void shouldMapAllAdditionalDocumentTypes() {
        // Given
        Document document = Document.builder()
            .filename("test.pdf")
            .build();

        AdditionalDocument witnessStatement = AdditionalDocument.builder()
            .description("Witness statement")
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document)
            .build();

        AdditionalDocument rentStatement = AdditionalDocument.builder()
            .description("Rent statement")
            .documentType(AdditionalDocumentType.RENT_STATEMENT)
            .document(document)
            .build();

        AdditionalDocument tenancyAgreement = AdditionalDocument.builder()
            .description("Tenancy agreement")
            .documentType(AdditionalDocumentType.TENANCY_AGREEMENT)
            .document(document)
            .build();

        AdditionalDocument letterFromClaimant = AdditionalDocument.builder()
            .description("Letter from claimant")
            .documentType(AdditionalDocumentType.LETTER_FROM_CLAIMANT)
            .document(document)
            .build();

        AdditionalDocument statementOfService = AdditionalDocument.builder()
            .description("Statement of service")
            .documentType(AdditionalDocumentType.STATEMENT_OF_SERVICE)
            .document(document)
            .build();

        AdditionalDocument videoEvidence = AdditionalDocument.builder()
            .description("Video evidence")
            .documentType(AdditionalDocumentType.VIDEO_EVIDENCE)
            .document(document)
            .build();

        AdditionalDocument photographicEvidence = AdditionalDocument.builder()
            .description("Photographic evidence")
            .documentType(AdditionalDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = Arrays.asList(
            ListValue.<AdditionalDocument>builder().id("1").value(witnessStatement).build(),
            ListValue.<AdditionalDocument>builder().id("2").value(rentStatement).build(),
            ListValue.<AdditionalDocument>builder().id("3").value(tenancyAgreement).build(),
            ListValue.<AdditionalDocument>builder().id("4").value(letterFromClaimant).build(),
            ListValue.<AdditionalDocument>builder().id("5").value(statementOfService).build(),
            ListValue.<AdditionalDocument>builder().id("6").value(videoEvidence).build(),
            ListValue.<AdditionalDocument>builder().id("7").value(photographicEvidence).build()
        );

        when(pcsCase.getCitizenDocuments()).thenReturn(citizenDocuments);

        // When
        List<PartyDocumentDto> result = citizenDocumentsService.buildCitizenDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(7);

        assertThat(result)
            .extracting(PartyDocumentDto::getDocumentType)
            .containsExactlyInAnyOrder(
                AdditionalDocumentType.WITNESS_STATEMENT,
                AdditionalDocumentType.RENT_STATEMENT,
                AdditionalDocumentType.TENANCY_AGREEMENT,
                AdditionalDocumentType.LETTER_FROM_CLAIMANT,
                AdditionalDocumentType.STATEMENT_OF_SERVICE,
                AdditionalDocumentType.VIDEO_EVIDENCE,
                AdditionalDocumentType.PHOTOGRAPHIC_EVIDENCE
            );
    }

}
