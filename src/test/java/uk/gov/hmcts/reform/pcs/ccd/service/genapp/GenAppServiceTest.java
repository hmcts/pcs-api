package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppServiceTest {

    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, 8, 27)
            .atTime(12, 51, 19);

    @Mock
    private GenAppRepository genAppRepository;
    @Mock(strictness = LENIENT)
    private DocumentRepository documentRepository;
    @Mock
    private Clock utcClock;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private PartyEntity applicantParty;
    @Captor
    private ArgumentCaptor<GenAppEntity> genAppEntityCaptor;
    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentEntityListCaptor;

    private GenAppService underTest;

    @BeforeEach
    void setUp() {
        stubUtcClock(TEST_UTC_DATE_TIME);

        underTest = new GenAppService(genAppRepository, documentRepository, utcClock);
    }

    @Test
    void shouldReturnSavedEntity() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(GenAppType.SOMETHING_ELSE)
            .build();

        GenAppEntity savedGenAppEntity = mock(GenAppEntity.class);
        when(genAppRepository.save(isA(GenAppEntity.class))).thenReturn(savedGenAppEntity);

        // When
        GenAppEntity returnedGenAppEntity = underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(any(GenAppEntity.class));
        assertThat(returnedGenAppEntity).isSameAs(savedGenAppEntity);
    }

    @Test
    void shouldAddSavedEntityToPcsCaseEntity() {
        // Given
        GenAppType genAppType = GenAppType.SOMETHING_ELSE;
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(genAppType)
            .build();

        GenAppEntity savedGenAppEntity = mock(GenAppEntity.class);
        when(genAppRepository.save(isA(GenAppEntity.class))).thenReturn(savedGenAppEntity);

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(pcsCaseEntity).addGenApp(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getType()).isEqualTo(genAppType);
    }

    @ParameterizedTest
    @EnumSource(value = GenAppType.class)
    void shouldSetGeneralApplicationType(GenAppType genAppType) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(genAppType)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getType()).isEqualTo(genAppType);
    }

    @Test
    void shouldSetInitialState() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getState()).isEqualTo(GenAppState.SUBMITTED);
    }

    @Test
    void shouldSetApplicantParty() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .applicationType(GenAppType.SUSPEND)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getParty()).isEqualTo(applicantParty);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldSetWithin14DaysFlag(VerticalYesNo within14Days) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .within14Days(within14Days)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getWithin14Days()).isEqualTo(within14Days);
    }

    @Test
    void shouldSetHwfDetails() {
        // Given
        String expectedHwfReference = "hwf-1234";
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.YES)
            .hwfReference(expectedHwfReference)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();

        assertThat(genAppEntity.getNeedHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(genAppEntity.getAppliedForHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(genAppEntity.getHelpWithFeesEntity().getHwfReference()).isEqualTo(expectedHwfReference);
    }

    @Test
    void shouldNotSetHwfReferenceIfAppliedForIsNo() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.NO)
            .hwfReference("hwf-1234")
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();

        assertThat(genAppEntity.getNeedHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(genAppEntity.getAppliedForHwf()).isEqualTo(VerticalYesNo.NO);
        assertThat(genAppEntity.getHelpWithFeesEntity()).isNull();
    }

    @ParameterizedTest
    @MethodSource("otherPartiesAgreedScenarios")
    void shouldSetOtherPartiesAgreedDetails(CitizenGenAppRequest genAppRequest,
                                            VerticalYesNo expectedOtherPartiesAgreed,
                                            VerticalYesNo expectedWithoutNotice,
                                            String expectedWithoutNoticeReason) {

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();

        assertThat(genAppEntity.getOtherPartiesAgreed()).isEqualTo(expectedOtherPartiesAgreed);
        assertThat(genAppEntity.getWithoutNotice()).isEqualTo(expectedWithoutNotice);
        assertThat(genAppEntity.getWithoutNoticeReason()).isEqualTo(expectedWithoutNoticeReason);
    }

    @Test
    void shouldSetWhatOrderWanted() {
        // Given
        String expectedOrder = "this is the order wanted";

        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .whatOrderWanted(expectedOrder)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getWhatOrderWanted()).isEqualTo(expectedOrder);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldSetSupportingDocumentsFlag(VerticalYesNo hasSupportingDocuments) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .hasSupportingDocuments(hasSupportingDocuments)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getDocumentsUploaded()).isEqualTo(hasSupportingDocuments);
    }

    @Test
    void shouldSaveUploadedDocuments() {
        // Given
        Document document = Document.builder()
            .filename("test filename")
            .url("test url")
            .binaryUrl("test binary url")
            .build();

        UploadedDocument uploadedDocument = UploadedDocument.builder()
            .document(document)
            .contentType("test content type")
            .size(1234L)
            .build();

        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .hasSupportingDocuments(VerticalYesNo.YES)
            .uploadedDocuments(List.of(uploadedDocument))
            .build();

        List<DocumentEntity> savedDocumentEntities = List.of(mock(DocumentEntity.class));
        when(documentRepository.saveAll(anyList())).thenReturn(savedDocumentEntities);

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();
        assertThat(genAppEntity.getDocuments()).isEqualTo(savedDocumentEntities);

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());

        assertThat(documentEntityListCaptor.getValue()).hasSize(1);
        DocumentEntity documentEntity = documentEntityListCaptor.getValue().getFirst();
        assertThat(documentEntity.getFileName()).isEqualTo("test filename");
        assertThat(documentEntity.getUrl()).isEqualTo("test url");
        assertThat(documentEntity.getBinaryUrl()).isEqualTo("test binary url");
        assertThat(documentEntity.getCategoryId()).isEqualTo(CaseFileCategory.UNCATEGORISED.getId());
        assertThat(documentEntity.getContentType()).isEqualTo("test content type");
        assertThat(documentEntity.getSize()).isEqualTo(1234L);
    }

    @Test
    void shouldNotSaveUploadedDocumentsIfSupportingDocumentsFlagIsNo() {
        // Given
        Document document = Document.builder()
            .filename("test filename")
            .url("test url")
            .binaryUrl("test binary url")
            .build();

        UploadedDocument uploadedDocument = UploadedDocument.builder()
            .document(document)
            .contentType("test content type")
            .size(1234L)
            .build();

        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .hasSupportingDocuments(VerticalYesNo.NO)
            .uploadedDocuments(List.of(uploadedDocument))
            .build();

        List<DocumentEntity> savedDocumentEntities = List.of(mock(DocumentEntity.class));
        when(documentRepository.saveAll(anyList())).thenReturn(savedDocumentEntities);

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        GenAppEntity genAppEntity = genAppEntityCaptor.getValue();
        assertThat(genAppEntity.getDocuments()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class)
    void shouldSetLanguageUsed(LanguageUsed languageUsed) {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .languageUsed(languageUsed)
                .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getLanguageUsed()).isEqualTo(languageUsed);
    }

    @Test
    void shouldSetApplicationSubmittedDate() {
        // Given
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());
        assertThat(genAppEntityCaptor.getValue().getApplicationSubmittedDate()).isEqualTo(TEST_UTC_DATE_TIME);
    }

    @Test
    void shouldCreateAndSetStatementOfTruth() {
        // Given
        String expectedFullName = "Expected full name";
        CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
            .sotAccepted(VerticalYesNo.YES)
            .sotFullName(expectedFullName)
            .build();

        // When
        underTest.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

        // Then
        verify(genAppRepository).save(genAppEntityCaptor.capture());

        StatementOfTruthEntity statementOfTruth = genAppEntityCaptor.getValue().getStatementOfTruth();
        assertThat(statementOfTruth.getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(statementOfTruth.getFullName()).isEqualTo(expectedFullName);
        assertThat(statementOfTruth.getCompletedDate()).isEqualTo(TEST_UTC_DATE_TIME);
    }

    private static Stream<Arguments> otherPartiesAgreedScenarios() {
        return Stream.of(
            argumentSet("Parties agreed", CitizenGenAppRequest.builder()
                            .otherPartiesAgreed(VerticalYesNo.YES)
                            .withoutNotice(VerticalYesNo.YES)         // Should be ignored
                            .withoutNoticeReason("some reason") // Should be ignored
                            .build(),
                        VerticalYesNo.YES,    // Expected otherPartiesAgreed
                        null,           // Expected withoutNotice
                        null            // Expected withoutNoticeReason
            ),
            argumentSet("Parties not agreed, without notice", CitizenGenAppRequest.builder()
                            .otherPartiesAgreed(VerticalYesNo.NO)
                            .withoutNotice(VerticalYesNo.YES)
                            .withoutNoticeReason("some reason")
                            .build(),
                        VerticalYesNo.NO,     // Expected otherPartiesAgreed
                        VerticalYesNo.YES,    // Expected withoutNotice
                        "some reason"   // Expected withoutNoticeReason
            ),
            argumentSet("Parties not agreed, not without notice", CitizenGenAppRequest.builder()
                            .otherPartiesAgreed(VerticalYesNo.NO)
                            .withoutNotice(VerticalYesNo.NO)
                            .withoutNoticeReason("some reason") // Should be ignored
                            .build(),
                        VerticalYesNo.NO,     // Expected otherPartiesAgreed
                        VerticalYesNo.NO,     // Expected withoutNotice
                        null            // Expected withoutNoticeReason
            )
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void stubUtcClock(LocalDateTime fixedTestDate) {
        when(utcClock.instant()).thenReturn(fixedTestDate.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
    }

}
