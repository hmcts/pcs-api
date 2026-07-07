package uk.gov.hmcts.reform.pcs.ccd.service.caseworker;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@ExtendWith(MockitoExtension.class)
class CaseworkerDocumentServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID SELECTED_PARTY_ID = UUID.randomUUID();
    private static final UUID SELECTED_GEN_APP_ID = UUID.randomUUID();
    private static final UUID SELECTED_COUNTERCLAIM_ID = UUID.randomUUID();

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private DocumentService documentService;
    @Mock
    private GenAppService genAppService;
    @Mock
    private DocumentIdExtractor documentIdExtractor;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentNameService documentNameService;
    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaim;
    @Captor
    private ArgumentCaptor<DocumentEntity> documentEntityCaptor;

    private CaseworkerDocumentService underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getMainClaim()).thenReturn(mainClaim);

        underTest = new CaseworkerDocumentService(pcsCaseService, documentService, genAppService, documentIdExtractor,
                                                  documentRepository, documentNameService, counterClaimRepository,
                                                  partyRepository
        );
    }

    @ParameterizedTest
    @MethodSource("documentTypeMappingScenarios")
    void shouldSetDocumentTypeWhenRelatedSubmissionIsNone(CaseworkerDocumentType caseworkerDocumentType,
                                                          DocumentType expectedDocumentType) {
        // Given
        DynamicStringList relatedSubmissionsDocumentTypeList
            = dynamicStringListWithSelection(caseworkerDocumentType.name());

        DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

        Document document = Document.builder()
            .build();

        DynamicStringList relatedSubmissionList = dynamicStringListWithSelection(NONE_PREFIX);

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .document(document)
            .showRelatedSubmissionsList(VerticalYesNo.YES)
            .relatedSubmission(relatedSubmissionList)
            .relatedSubmissionsDocumentType(relatedSubmissionsDocumentTypeList)
            .relatedParty(relatedPartyList)
            .build();

        // When
        underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

        // Then
        DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
        assertThat(savedDocumentEntity.getType()).isEqualTo(expectedDocumentType);
    }

    @Test
    void shouldNotSetDocumentTypeWhenRelatedSubmissionSelected() {
        // Given
        DynamicStringList relatedSubmissionsDocumentTypeList
            = dynamicStringListWithSelection(CaseworkerDocumentType.AMENDED_CLAIM_FORM.name());

        DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

        Document document = Document.builder()
            .build();

        DynamicStringList relatedSubmissionList
            = dynamicStringListWithSelection(GEN_APP_ID_PREFIX + ":" + SELECTED_GEN_APP_ID);

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .document(document)
            .showRelatedSubmissionsList(VerticalYesNo.YES)
            .relatedSubmission(relatedSubmissionList)
            .relatedSubmissionsDocumentType(relatedSubmissionsDocumentTypeList)
            .relatedParty(relatedPartyList)
            .build();

        // When
        underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

        // Then
        DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
        assertThat(savedDocumentEntity.getType()).isNull();
    }

    @Test
    void shouldNotModifyNameOrSetCategoryForUnknownRelatedSubmissionPrefix() {
        // Given
        DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

        String originalFilename = "original filename.pdf";
        Document document = Document.builder()
            .filename(originalFilename)
            .build();

        DynamicStringList relatedSubmissionList
            = dynamicStringListWithSelection("UNKNOWN_PREFIX:" + SELECTED_GEN_APP_ID);

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .document(document)
            .showRelatedSubmissionsList(VerticalYesNo.YES)
            .relatedSubmission(relatedSubmissionList)
            .relatedParty(relatedPartyList)
            .build();

        // When
        underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

        // Then
        DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
        assertThat(savedDocumentEntity.getFileName()).isEqualTo(originalFilename);
        assertThat(savedDocumentEntity.getCategoryId()).isNull();
    }

    @ParameterizedTest
    @MethodSource("documentTypeMappingScenarios")
    void shouldSetDocumentTypeWhenRelatedSubmissionsNotShown(CaseworkerDocumentType caseworkerDocumentType,
                                                             DocumentType expectedDocumentType) {
        // Given
        DynamicStringList standaloneDocumentTypeList
            = dynamicStringListWithSelection(caseworkerDocumentType.name());

        DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

        Document document = Document.builder()
            .build();

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .document(document)
            .showRelatedSubmissionsList(VerticalYesNo.NO)
            .standaloneDocumentType(standaloneDocumentTypeList)
            .relatedParty(relatedPartyList)
            .build();

        // When
        underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

        // Then
        DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
        assertThat(savedDocumentEntity.getType()).isEqualTo(expectedDocumentType);
    }

    @Test
    void shouldReturnSavedEntity() {
        // Given
        DynamicStringList relatedSubmissionsDocumentTypeList
            = dynamicStringListWithSelection(CaseworkerDocumentType.OCCUPATION_LICENCE.name());

        DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

        Document document = Document.builder()
            .build();

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .document(document)
            .showRelatedSubmissionsList(VerticalYesNo.NO)
            .standaloneDocumentType(relatedSubmissionsDocumentTypeList)
            .relatedParty(relatedPartyList)
            .build();

        DocumentEntity savedDocumentEntity = mock(DocumentEntity.class);
        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedDocumentEntity);

        // When
        DocumentEntity returnedDocumentEntity = underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

        // Then
        assertThat(returnedDocumentEntity).isSameAs(savedDocumentEntity);
    }

    @Nested
    @DisplayName("Document filename tests")
    class FilenameTests {

        @Test
        void shouldSetFilenameForGenApp() {
            // Given
            DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

            String originalFilename = "test-filename";
            String modifiedFilenameForGenApp = "filename for gen app";

            Document document = createDocument(originalFilename);

            DynamicStringList relatedSubmissionList
                = dynamicStringListWithSelection(GEN_APP_ID_PREFIX + ":" + SELECTED_GEN_APP_ID);

            GenAppEntity genAppEntity = mock(GenAppEntity.class);

            when(genAppService.loadGenApp(SELECTED_GEN_APP_ID)).thenReturn(genAppEntity);
            when(documentNameService.appendGenAppPostfix(originalFilename, genAppEntity, mainClaim, SELECTED_PARTY_ID))
                .thenReturn(modifiedFilenameForGenApp);

            CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
                .document(document)
                .showRelatedSubmissionsList(VerticalYesNo.YES)
                .relatedSubmission(relatedSubmissionList)
                .relatedParty(relatedPartyList)
                .build();

            // When
            underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

            // Then
            DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
            assertThat(savedDocumentEntity.getFileName()).isEqualTo(modifiedFilenameForGenApp);
            assertThat(savedDocumentEntity.getGeneralApplication()).isEqualTo(genAppEntity);
        }

        @Test
        void shouldSetFilenameForGenAppWithDate() {
            // Given
            DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

            String originalFilename = "test-filename";
            String filenameWithDate = "filename with date";
            String modifiedFilenameForGenApp = "filename with date for gen app";

            Document document = createDocument(originalFilename);

            DynamicStringList relatedSubmissionList
                = dynamicStringListWithSelection(GEN_APP_ID_PREFIX + ":" + SELECTED_GEN_APP_ID);

            GenAppEntity genAppEntity = mock(GenAppEntity.class);

            LocalDate documentIssueDate = mock(LocalDate.class);
            when(documentNameService.appendDate(originalFilename, documentIssueDate))
                .thenReturn(filenameWithDate);

            when(genAppService.loadGenApp(SELECTED_GEN_APP_ID)).thenReturn(genAppEntity);
            when(documentNameService.appendGenAppPostfix(filenameWithDate, genAppEntity, mainClaim, SELECTED_PARTY_ID))
                .thenReturn(modifiedFilenameForGenApp);

            CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
                .document(document)
                .issueDate(documentIssueDate)
                .showRelatedSubmissionsList(VerticalYesNo.YES)
                .relatedSubmission(relatedSubmissionList)
                .relatedParty(relatedPartyList)
                .build();

            // When
            underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

            // Then
            DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
            assertThat(savedDocumentEntity.getFileName()).isEqualTo(modifiedFilenameForGenApp);
            assertThat(savedDocumentEntity.getGeneralApplication()).isEqualTo(genAppEntity);
        }

        @Test
        void shouldSetFilenameForCounterclaim() {
            // Given
            DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

            String originalFilename = "test-filename";
            String modifiedFilenameForCounterclaim = "filename for counterclaim";

            Document document = createDocument(originalFilename);

            DynamicStringList relatedSubmissionList
                = dynamicStringListWithSelection(COUNTERCLAIM_ID_PREFIX + ":" + SELECTED_COUNTERCLAIM_ID);

            CounterClaimEntity counterClaimEntity = mock(CounterClaimEntity.class);

            when(counterClaimRepository.getReferenceById(SELECTED_COUNTERCLAIM_ID)).thenReturn(counterClaimEntity);
            when(documentNameService.appendCounterClaimPostfix(originalFilename, mainClaim, SELECTED_PARTY_ID))
                .thenReturn(modifiedFilenameForCounterclaim);

            CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
                .document(document)
                .showRelatedSubmissionsList(VerticalYesNo.YES)
                .relatedSubmission(relatedSubmissionList)
                .relatedParty(relatedPartyList)
                .build();

            // When
            underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

            // Then
            DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
            assertThat(savedDocumentEntity.getFileName()).isEqualTo(modifiedFilenameForCounterclaim);
            assertThat(savedDocumentEntity.getCounterClaim()).isEqualTo(counterClaimEntity);
        }

        @Test
        void shouldSetFilenameForCounterclaimWithDate() {
            // Given
            DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

            String originalFilename = "test-filename";
            String filenameWithDate = "filename with date";
            String modifiedFilenameForCounterclaim = "filename with date for counterclaim";

            Document document = createDocument(originalFilename);

            DynamicStringList relatedSubmissionList
                = dynamicStringListWithSelection(COUNTERCLAIM_ID_PREFIX + ":" + SELECTED_COUNTERCLAIM_ID);

            CounterClaimEntity counterClaimEntity = mock(CounterClaimEntity.class);

            LocalDate documentIssueDate = mock(LocalDate.class);
            when(documentNameService.appendDate(originalFilename, documentIssueDate))
                .thenReturn(filenameWithDate);

            when(counterClaimRepository.getReferenceById(SELECTED_COUNTERCLAIM_ID)).thenReturn(counterClaimEntity);
            when(documentNameService.appendCounterClaimPostfix(filenameWithDate, mainClaim, SELECTED_PARTY_ID))
                .thenReturn(modifiedFilenameForCounterclaim);

            CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
                .document(document)
                .issueDate(documentIssueDate)
                .showRelatedSubmissionsList(VerticalYesNo.YES)
                .relatedSubmission(relatedSubmissionList)
                .relatedParty(relatedPartyList)
                .build();

            // When
            underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

            // Then
            DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
            assertThat(savedDocumentEntity.getFileName()).isEqualTo(modifiedFilenameForCounterclaim);
        }

        @Test
        void shouldSetFilenameForStandaloneDocument() {
            // Given
            DynamicStringList relatedSubmissionsDocumentTypeList
                = dynamicStringListWithSelection(CaseworkerDocumentType.OCCUPATION_LICENCE.name());

            DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

            String originalFilename = "test-filename";
            String modifiedFilenameForParty = "filename for party";

            Document document = createDocument(originalFilename);

            when(documentNameService.appendPartyPostfix(originalFilename, mainClaim, SELECTED_PARTY_ID))
                .thenReturn(modifiedFilenameForParty);

            CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
                .document(document)
                .showRelatedSubmissionsList(VerticalYesNo.NO)
                .standaloneDocumentType(relatedSubmissionsDocumentTypeList)
                .relatedParty(relatedPartyList)
                .build();

            // When
            underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

            // Then
            DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
            assertThat(savedDocumentEntity.getFileName()).isEqualTo(modifiedFilenameForParty);
        }

        @Test
        void shouldSetFilenameForStandaloneWithDate() {
            // Given
            DynamicStringList relatedSubmissionsDocumentTypeList
                = dynamicStringListWithSelection(CaseworkerDocumentType.OCCUPATION_LICENCE.name());

            DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

            String originalFilename = "test-filename";
            String filenameWithDate = "filename with date";
            String modifiedFilenameForParty = "filename with date for party";

            Document document = createDocument(originalFilename);

            LocalDate documentIssueDate = mock(LocalDate.class);
            when(documentNameService.appendDate(originalFilename, documentIssueDate))
                .thenReturn(filenameWithDate);
            when(documentNameService.appendPartyPostfix(filenameWithDate, mainClaim, SELECTED_PARTY_ID))
                .thenReturn(modifiedFilenameForParty);

            CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
                .document(document)
                .issueDate(documentIssueDate)
                .showRelatedSubmissionsList(VerticalYesNo.NO)
                .standaloneDocumentType(relatedSubmissionsDocumentTypeList)
                .relatedParty(relatedPartyList)
                .build();

            // When
            underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

            // Then
            DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
            assertThat(savedDocumentEntity.getFileName()).isEqualTo(modifiedFilenameForParty);
        }

    }

    @Test
    void shouldSetOtherDocumentProperties() {
        // Given
        DynamicStringList relatedSubmissionsDocumentTypeList
            = dynamicStringListWithSelection(CaseworkerDocumentType.OCCUPATION_LICENCE.name());

        DynamicList relatedPartyList = dynamicListWithSelection(SELECTED_PARTY_ID);

        String documentUrl = "document URL";
        String documentBinaryUrl = "document binary URL";

        Document document = Document.builder()
            .url(documentUrl)
            .binaryUrl(documentBinaryUrl)
            .build();

        UUID documentId = UUID.randomUUID();
        CaseFileCategory documentCategory = CaseFileCategory.PROPERTY_DOCUMENTS;
        when(documentIdExtractor.extractDocumentId(documentUrl)).thenReturn(documentId);
        when(documentService.mapDocumentTypeToCategory(DocumentType.OCCUPATION_LICENCE))
            .thenReturn(Optional.of(documentCategory));

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .document(document)
            .showRelatedSubmissionsList(VerticalYesNo.NO)
            .standaloneDocumentType(relatedSubmissionsDocumentTypeList)
            .relatedParty(relatedPartyList)
            .build();

        // When
        underTest.saveNewDocument(caseworkerDocument, CASE_REFERENCE);

        // Then
        DocumentEntity savedDocumentEntity = getSavedDocumentEntity();
        assertThat(savedDocumentEntity.getUrl()).isEqualTo(documentUrl);
        assertThat(savedDocumentEntity.getBinaryUrl()).isEqualTo(documentBinaryUrl);
        assertThat(savedDocumentEntity.getDocumentId()).isEqualTo(documentId);
        assertThat(savedDocumentEntity.getCategoryId()).isEqualTo(documentCategory.getId());
    }

    private static Stream<Arguments> documentTypeMappingScenarios() {
        return Arrays.stream(CaseworkerDocumentType.values())
            .map(caseworkerDocumentType -> {
                DocumentType expectedDocumentType = DocumentType.valueOf(caseworkerDocumentType.name());
                return arguments(caseworkerDocumentType, expectedDocumentType);
            });
    }

    private static DynamicStringList dynamicStringListWithSelection(String selectedCode) {
        return DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(selectedCode).build())
            .build();
    }

    @SuppressWarnings("SameParameterValue")
    private static DynamicList dynamicListWithSelection(UUID selectedCode) {
        return DynamicList.builder()
            .value(DynamicListElement.builder().code(selectedCode).build())
            .build();
    }

    private static Document createDocument(String originalFilename) {
        return Document.builder()
            .filename(originalFilename)
            .build();
    }

    private DocumentEntity getSavedDocumentEntity() {
        verify(documentRepository).save(documentEntityCaptor.capture());
        return documentEntityCaptor.getValue();
    }

}
