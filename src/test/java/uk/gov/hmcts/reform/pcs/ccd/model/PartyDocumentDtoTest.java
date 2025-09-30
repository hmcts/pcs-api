package uk.gov.hmcts.reform.pcs.ccd.model;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;

import static org.assertj.core.api.Assertions.assertThat;

class PartyDocumentDtoTest {

    @Test
    void shouldBuildPartyDocumentDtoWithAllFields() {
        // Given
        String description = "Test document description";
        AdditionalDocumentType documentType = AdditionalDocumentType.RENT_STATEMENT;
        Document document = Document.builder()
            .filename("test_document.pdf")
            .url("http://example.com/test_document.pdf")
            .build();

        // When
        PartyDocumentDto result = PartyDocumentDto.builder()
            .description(description)
            .documentType(documentType)
            .document(document)
            .build();

        // Then
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getDocumentType()).isEqualTo(documentType);
        assertThat(result.getDocument()).isEqualTo(document);
    }

    @Test
    void shouldHandleNullValues() {
        // When
        PartyDocumentDto result = PartyDocumentDto.builder()
            .description(null)
            .documentType(null)
            .document(null)
            .build();

        // Then
        assertThat(result.getDescription()).isNull();
        assertThat(result.getDocumentType()).isNull();
        assertThat(result.getDocument()).isNull();
    }

    @Test
    void shouldTestBuilderPattern() {
        // Given
        String description = "Rent receipt for January 2025";
        AdditionalDocumentType documentType = AdditionalDocumentType.RENT_STATEMENT;
        Document document = Document.builder()
            .filename("rent_receipt_jan.pdf")
            .url("http://example.com/rent_receipt_jan.pdf")
            .build();

        // When
        PartyDocumentDto result = PartyDocumentDto.builder()
            .description(description)
            .documentType(documentType)
            .document(document)
            .build();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getDocumentType()).isEqualTo(documentType);
        assertThat(result.getDocument()).isEqualTo(document);
    }

    @Test
    void shouldTestSettersAndGetters() {
        // Given
        PartyDocumentDto partyDocumentDto = new PartyDocumentDto();
        String description = "Test description";
        AdditionalDocumentType documentType = AdditionalDocumentType.TENANCY_AGREEMENT;
        Document document = Document.builder()
            .filename("tenancy_agreement.pdf")
            .url("http://example.com/tenancy_agreement.pdf")
            .build();

        // When
        partyDocumentDto.setDescription(description);
        partyDocumentDto.setDocumentType(documentType);
        partyDocumentDto.setDocument(document);

        // Then
        assertThat(partyDocumentDto.getDescription()).isEqualTo(description);
        assertThat(partyDocumentDto.getDocumentType()).isEqualTo(documentType);
        assertThat(partyDocumentDto.getDocument()).isEqualTo(document);
    }

    @Test
    void shouldTestEqualsAndHashCode() {
        // Given
        String description = "Test description";
        AdditionalDocumentType documentType = AdditionalDocumentType.WITNESS_STATEMENT;
        Document document = Document.builder()
            .filename("court_order.pdf")
            .url("http://example.com/court_order.pdf")
            .build();

        PartyDocumentDto dto1 = PartyDocumentDto.builder()
            .description(description)
            .documentType(documentType)
            .document(document)
            .build();

        PartyDocumentDto dto2 = PartyDocumentDto.builder()
            .description(description)
            .documentType(documentType)
            .document(document)
            .build();

        // When & Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }
}
