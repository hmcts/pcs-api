package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingApplicationDocumentLinkBuilderTest {

    private static final UUID PCS_DOCUMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID DM_STORE_DOCUMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String PPTX_CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    @Mock
    private PartyService partyService;

    @ParameterizedTest
    @MethodSource("supportedDocumentTypes")
    void shouldUseXuiDocumentRouteForSupportedDocumentTypes(String fileName, String contentType) {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);
        String binaryUrl = "http://dm-store/documents/%s/binary".formatted(DM_STORE_DOCUMENT_ID);

        DocumentEntity document = DocumentEntity.builder()
            .id(PCS_DOCUMENT_ID)
            .documentId(DM_STORE_DOCUMENT_ID)
            .url("http://dm-store/documents/" + DM_STORE_DOCUMENT_ID)
            .binaryUrl(binaryUrl)
            .fileName(fileName)
            .contentType(contentType)
            .build();

        String links = underTest.build(pcsCase(), List.of(genApp(document)));

        assertThat(links).contains("href=\"/documents/" + DM_STORE_DOCUMENT_ID + "/binary\"");
        assertThat(links).doesNotContain(binaryUrl);
        assertThat(links).doesNotContain(PCS_DOCUMENT_ID.toString());
        assertThat(links).doesNotContain("dm-store");
    }

    @Test
    void shouldExtractDocumentIdFromUrlForLegacyRecords() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);

        DocumentEntity document = DocumentEntity.builder()
            .id(PCS_DOCUMENT_ID)
            .url("http://dm-store/documents/" + DM_STORE_DOCUMENT_ID)
            .build();

        String links = underTest.build(pcsCase(), List.of(genApp(document)));

        assertThat(links).contains("href=\"/documents/" + DM_STORE_DOCUMENT_ID + "/binary\"");
    }

    @Test
    void shouldExtractDocumentIdFromBinaryUrlForLegacyRecords() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);

        DocumentEntity document = DocumentEntity.builder()
            .id(PCS_DOCUMENT_ID)
            .binaryUrl("http://dm-store/documents/" + DM_STORE_DOCUMENT_ID + "/binary")
            .build();

        String links = underTest.build(PcsCaseEntity.builder().build(), List.of(genApp(document)));

        assertThat(links).contains("href=\"/documents/" + DM_STORE_DOCUMENT_ID + "/binary\"");
    }

    @Test
    void shouldReturnEmptyStringWhenNoApplicationHasDownloadableDocument() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);

        DocumentEntity documentWithoutId = DocumentEntity.builder()
            .url("http://dm-store/documents/not-a-uuid")
            .binaryUrl("http://dm-store/documents/not-a-uuid/binary")
            .build();
        DocumentEntity documentWithoutDocumentPath = DocumentEntity.builder()
            .url("http://dm-store/files/%s".formatted(DM_STORE_DOCUMENT_ID))
            .build();

        String links = underTest.build(
            pcsCase(),
            List.of(genApp(null), genApp(documentWithoutId), genApp(documentWithoutDocumentPath))
        );

        assertThat(links).isEmpty();
    }

    @Test
    void shouldUseRankFallbackWhenApplicationHasNoRank() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);

        GenAppEntity genApp = GenAppEntity.builder().build();

        assertThat(underTest.applicationLabel(pcsCase(), genApp)).isEqualTo("General app (GA)");
    }

    @Test
    void shouldOmitPartyLabelWhenApplicationPartyHasNoId() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);

        GenAppEntity genApp = GenAppEntity.builder()
            .rank(1)
            .party(PartyEntity.builder().build())
            .build();

        assertThat(underTest.applicationLabel(pcsCase(), genApp)).isEqualTo("General app (GA1)");
    }

    @Test
    void shouldAppendPartyLabelWhenApplicationHasParty() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);
        UUID partyId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .claims(List.of(mainClaim))
            .build();
        GenAppEntity genApp = GenAppEntity.builder()
            .rank(2)
            .party(PartyEntity.builder().id(partyId).build())
            .build();

        when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("Defendant 1");

        assertThat(underTest.applicationLabel(pcsCase, genApp))
            .isEqualTo("General app (GA2) - Defendant 1");
    }

    @Test
    void shouldOmitPartyLabelWhenPartyServiceReturnsNull() {
        ExistingApplicationDocumentLinkBuilder underTest = new ExistingApplicationDocumentLinkBuilder(partyService);
        UUID partyId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .claims(List.of(mainClaim))
            .build();
        GenAppEntity genApp = GenAppEntity.builder()
            .rank(3)
            .party(PartyEntity.builder().id(partyId).build())
            .build();

        when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn(null);

        assertThat(underTest.applicationLabel(pcsCase, genApp)).isEqualTo("General app (GA3)");
    }

    private static PcsCaseEntity pcsCase() {
        return PcsCaseEntity.builder().build();
    }

    private static GenAppEntity genApp(DocumentEntity document) {
        return GenAppEntity.builder()
            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .rank(1)
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(LocalDateTime.of(2026, 2, 1, 10, 0))
            .submissionDocument(document)
            .build();
    }

    private static Stream<Arguments> supportedDocumentTypes() {
        return Stream.of(
            Arguments.of("application.doc", "application/msword"),
            Arguments.of("application.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Arguments.of("application.xls", "application/vnd.ms-excel"),
            Arguments.of("application.xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12"),
            Arguments.of("application.ppt", "application/vnd.ms-powerpoint"),
            Arguments.of("application.pptx", PPTX_CONTENT_TYPE),
            Arguments.of("application.pdf", "application/pdf"),
            Arguments.of("application.rtf", "application/rtf"),
            Arguments.of("application.txt", "text/plain"),
            Arguments.of("application.csv", "text/csv"),
            Arguments.of("application.jpg", "image/jpeg"),
            Arguments.of("application.jpeg", "image/jpeg"),
            Arguments.of("application.png", "image/png"),
            Arguments.of("application.bmp", "image/bmp"),
            Arguments.of("application.tif", "image/tiff"),
            Arguments.of("application.tiff", "image/tiff")
        );
    }
}
