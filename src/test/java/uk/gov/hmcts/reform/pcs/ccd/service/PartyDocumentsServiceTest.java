package uk.gov.hmcts.reform.pcs.ccd.service;

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
class PartyDocumentsServiceTest {

    private final PartyDocumentsService partyDocumentsService =
        new PartyDocumentsService();

    @Mock
    private PCSCase pcsCase;

    @Test
    void shouldBuildPartyDocumentsFromAdditionalDocuments() {
        // Given
        Document document1 = Document.builder()
            .filename("witness_statement.pdf")
            .build();

        Document document2 = Document.builder()
            .filename("rent_statement.pdf")
            .build();

        AdditionalDocument additionalDocument1 = AdditionalDocument.builder()
            .description("Witness statement from John Doe")
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document1)
            .build();

        AdditionalDocument additionalDocument2 = AdditionalDocument.builder()
            .description("Rent statement for January 2024")
            .documentType(AdditionalDocumentType.RENT_STATEMENT)
            .document(document2)
            .build();

        List<ListValue<AdditionalDocument>> additionalDocuments =
            Arrays.asList(
                ListValue.<AdditionalDocument>builder()
                    .id("1")
                    .value(additionalDocument1)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("2")
                    .value(additionalDocument2)
                    .build()
            );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(2);

        assertThat(result)
            .extracting(PartyDocumentDto::getDescription)
            .containsExactlyInAnyOrder(
                "Witness statement from John Doe",
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
    void shouldHandleEmptyAdditionalDocuments() {
        // Given
        when(pcsCase.getAdditionalDocuments()).thenReturn(
            Collections.emptyList()
        );

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleNullAdditionalDocuments() {
        // Given
        when(pcsCase.getAdditionalDocuments()).thenReturn(null);

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

        // Then
        assertThat(result).isEmpty();
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

        AdditionalDocument photographicEvidence =
            AdditionalDocument.builder()
                .description("Photographic evidence")
                .documentType(AdditionalDocumentType.PHOTOGRAPHIC_EVIDENCE)
                .document(document)
                .build();

        List<ListValue<AdditionalDocument>> additionalDocuments =
            Arrays.asList(
                ListValue.<AdditionalDocument>builder()
                    .id("1")
                    .value(witnessStatement)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("2")
                    .value(rentStatement)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("3")
                    .value(tenancyAgreement)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("4")
                    .value(letterFromClaimant)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("5")
                    .value(statementOfService)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("6")
                    .value(videoEvidence)
                    .build(),
                ListValue.<AdditionalDocument>builder()
                    .id("7")
                    .value(photographicEvidence)
                    .build()
            );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

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

    @Test
    void shouldHandleNullDocumentInAdditionalDocument() {
        // Given
        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .description("Document without file")
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(null)
            .build();

        List<ListValue<AdditionalDocument>> additionalDocuments =
            Arrays.asList(
                ListValue.<AdditionalDocument>builder()
                    .id("1")
                    .value(additionalDocument)
                    .build()
            );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription())
            .isEqualTo("Document without file");
        assertThat(result.getFirst().getDocumentType())
            .isEqualTo(AdditionalDocumentType.WITNESS_STATEMENT);
        assertThat(result.getFirst().getDocument()).isNull();
    }

    @Test
    void shouldHandleNullDescriptionInAdditionalDocument() {
        // Given
        Document document = Document.builder()
            .filename("test.pdf")
            .build();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .description(null)
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> additionalDocuments =
            Arrays.asList(
                ListValue.<AdditionalDocument>builder()
                    .id("1")
                    .value(additionalDocument)
                    .build()
            );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isNull();
        assertThat(result.getFirst().getDocumentType())
            .isEqualTo(AdditionalDocumentType.WITNESS_STATEMENT);
        assertThat(result.getFirst().getDocument()).isEqualTo(document);
    }

    @Test
    void shouldHandleNullDocumentTypeInAdditionalDocument() {
        // Given
        Document document = Document.builder()
            .filename("test.pdf")
            .build();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .description("Document without type")
            .documentType(null)
            .document(document)
            .build();

        List<ListValue<AdditionalDocument>> additionalDocuments =
            Arrays.asList(
                ListValue.<AdditionalDocument>builder()
                    .id("1")
                    .value(additionalDocument)
                    .build()
            );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        List<PartyDocumentDto> result =
            partyDocumentsService.buildPartyDocuments(pcsCase);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription())
            .isEqualTo("Document without type");
        assertThat(result.getFirst().getDocumentType()).isNull();
        assertThat(result.getFirst().getDocument()).isEqualTo(document);
    }
}
