package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.FailureReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeFormDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeFormDocumentGenerator.LetterContext;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.service.TestAccessCodeRecorder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.DocumentLanguage.ENGLISH;
import static uk.gov.hmcts.reform.pcs.ccd.domain.DocumentLanguage.WELSH;

@ExtendWith(MockitoExtension.class)
class DefendantAccessCodeServiceTest {

    private static final String DOC_URL = "http://dm-store/documents/abc";
    private static final String WELSH_DOC_URL = "http://dm-store/documents/welsh";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepo;
    @Mock(strictness = LENIENT)
    private AccessCodeGenerator accessCodeGenerator;
    @Mock
    private PartyAccessCodeHashingService hashingService;
    @Mock(strictness = LENIENT)
    private AccessCodeFormDocumentGenerator accessCodeFormDocumentGenerator;
    @Mock
    private DocumentRepository documentRepository;
    @Mock(strictness = LENIENT)
    private DocumentIdExtractor documentIdExtractor;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;
    @Mock
    private TestAccessCodeRecorder testAccessCodeRecorder;

    private DefendantAccessCodeService underTest;

    @Captor
    private ArgumentCaptor<PartyAccessCodeEntity> accessCodeCaptor;
    @Captor
    private ArgumentCaptor<DocumentEntity> documentCaptor;
    @Captor
    private ArgumentCaptor<GenerationDetails> detailsCaptor;
    @Captor
    private ArgumentCaptor<String> plaintextCaptor;

    @BeforeEach
    void setUp() {
        underTest = new DefendantAccessCodeService(
            pcsCaseService,
            partyAccessCodeRepo,
            accessCodeGenerator,
            hashingService,
            accessCodeFormDocumentGenerator,
            documentRepository,
            documentIdExtractor,
            accessCodeActivityLogService,
            testAccessCodeRecorder
        );
        when(accessCodeGenerator.generateAccessCode()).thenCallRealMethod();
        lenient().when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), any(), any()))
            .thenReturn(DOC_URL);
        lenient().when(hashingService.encodeForStorage(anyString()))
            .thenAnswer(invocation -> "ENC-" + invocation.getArgument(0));
    }

    @Test
    void generateForDefendant_savesDocumentEncryptedCodePinAndLogsSuccess() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);

        underTest.generateForDefendant(1L, partyId, false, true);

        verify(documentRepository).save(documentCaptor.capture());
        DocumentEntity savedDoc = documentCaptor.getValue();
        assertThat(savedDoc.getType()).isEqualTo(DocumentType.DEFENDANT_ACCESS_CODE);
        assertThat(savedDoc.getCategoryId()).isEqualTo(CaseFileCategory.UNCATEGORISED_DOCUMENTS.getId());
        assertThat(savedDoc.getUrl()).isEqualTo(DOC_URL);
        assertThat(savedDoc.getParty().getId()).isEqualTo(partyId);
        assertThat(savedDoc.getPcsCase()).isEqualTo(caseEntity);
        assertThat(savedDoc.getFileName()).isEqualTo("Defendant Access Code " + partyId);
        assertThat(savedDoc.getLanguage()).isEqualTo(ENGLISH);
        verify(accessCodeFormDocumentGenerator).generate(any(), any(), any(), anyString(), eq(ENGLISH), any());
        verify(accessCodeFormDocumentGenerator, never()).generate(any(), any(), any(), anyString(), eq(WELSH), any());

        verify(partyAccessCodeRepo).save(accessCodeCaptor.capture());
        PartyAccessCodeEntity savedCode = accessCodeCaptor.getValue();
        assertThat(savedCode.getPartyId()).isEqualTo(partyId);
        assertThat(savedCode.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(savedCode.getCode()).startsWith("ENC-");

        verify(testAccessCodeRecorder).record(eq(caseEntity.getId()), eq(partyId), anyString());
        verify(accessCodeActivityLogService).logSuccess(caseEntity, savedDoc.getParty(),
                                                   ClaimActivityType.DOCUMENTS_CREATED);
        verify(accessCodeActivityLogService, never()).logFailure(any(), any(), any(), any());
    }

    @Test
    void generateForDefendant_finalAttemptFailure_logsFailureAndRethrows() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("docmosis down"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> underTest.generateForDefendant(5L, partyId, false, true));
        assertThat(thrown).hasMessage("docmosis down");

        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED),
                                                   any(GenerationDetails.class));
        verify(partyAccessCodeRepo, never()).save(any());
        verify(documentRepository, never()).save(any());
        verify(testAccessCodeRecorder, never()).record(any(), any(), anyString());
    }

    @Test
    void generateForDefendant_firstAttemptFailure_logsNonTerminalFailure() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("docmosis down"));

        assertThrows(RuntimeException.class, () -> underTest.generateForDefendant(5L, partyId, true, false));

        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED),
                                                   argThat((GenerationDetails d) -> !d.terminal()));
    }

    @Test
    void generateForDefendant_nonFinalAttemptFailure_doesNotLogFailure() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("docmosis down"));

        assertThrows(RuntimeException.class, () -> underTest.generateForDefendant(5L, partyId, false, false));

        verify(accessCodeActivityLogService, never()).logFailure(any(), any(), any(), any());
    }

    @Test
    void generateForDefendant_skipsWhenPartyAlreadyHasCode() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        when(partyAccessCodeRepo.existsByPcsCase_IdAndPartyId(caseEntity.getId(), partyId)).thenReturn(true);

        underTest.generateForDefendant(1L, partyId, false, true);

        verify(partyAccessCodeRepo, never()).save(any());
        verify(documentRepository, never()).save(any());
        verify(testAccessCodeRecorder, never()).record(any(), any(), anyString());
        verify(accessCodeActivityLogService, never()).logSuccess(any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = LegislativeCountry.class, names = "WALES", mode = EnumSource.Mode.EXCLUDE)
    void generateForDefendant_nonWalesCase_rendersOnlyTheEnglishLetter(LegislativeCountry country) {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        caseEntity.setLegislativeCountry(country);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);

        underTest.generateForDefendant(1L, partyId, false, true);

        verify(accessCodeFormDocumentGenerator).generate(any(), any(), any(), anyString(), eq(ENGLISH), any());
        verify(accessCodeFormDocumentGenerator, never()).generate(any(), any(), any(), anyString(), eq(WELSH), any());
        verify(documentRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getUrl()).isEqualTo(DOC_URL);
        assertThat(documentCaptor.getValue().getLanguage()).isEqualTo(ENGLISH);
        verify(partyAccessCodeRepo).save(any());
    }

    @Test
    void generateForDefendant_walesCase_rendersBothLettersWithOneCodeBeforeAnySave() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        stubBothLetters();

        underTest.generateForDefendant(1L, partyId, false, true);

        InOrder inOrder = inOrder(accessCodeFormDocumentGenerator, documentRepository, partyAccessCodeRepo);
        inOrder.verify(accessCodeFormDocumentGenerator)
            .generate(any(), any(), any(), plaintextCaptor.capture(), eq(ENGLISH), any());
        inOrder.verify(accessCodeFormDocumentGenerator)
            .generate(any(), any(), any(), plaintextCaptor.capture(), eq(WELSH), any());
        inOrder.verify(documentRepository, times(2)).save(any());
        inOrder.verify(partyAccessCodeRepo).save(accessCodeCaptor.capture());

        String plaintext = plaintextCaptor.getAllValues().getFirst();
        assertThat(plaintextCaptor.getAllValues()).containsExactly(plaintext, plaintext);
        assertThat(accessCodeCaptor.getValue().getCode()).isEqualTo("ENC-" + plaintext);
        verify(testAccessCodeRecorder).record(caseEntity.getId(), partyId, plaintext);
    }

    @Test
    void generateForDefendant_walesCase_savesEachLetterWithItsLanguage() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        stubBothLetters();

        underTest.generateForDefendant(1L, partyId, false, true);

        verify(documentRepository, times(2)).save(documentCaptor.capture());
        List<DocumentEntity> saved = documentCaptor.getAllValues();
        assertThat(saved.get(0).getUrl()).isEqualTo(DOC_URL);
        assertThat(saved.get(0).getFileName()).isEqualTo("Defendant Access Code " + partyId);
        assertThat(saved.get(0).getLanguage()).isEqualTo(ENGLISH);
        assertThat(saved.get(1).getUrl()).isEqualTo(WELSH_DOC_URL);
        assertThat(saved.get(1).getFileName()).isEqualTo("Defendant Access Code Welsh " + partyId);
        assertThat(saved.get(1).getLanguage()).isEqualTo(WELSH);
        assertThat(saved).allSatisfy(document -> {
            assertThat(document.getType()).isEqualTo(DocumentType.DEFENDANT_ACCESS_CODE);
            assertThat(document.getCategoryId()).isEqualTo(CaseFileCategory.UNCATEGORISED_DOCUMENTS.getId());
            assertThat(document.getParty().getId()).isEqualTo(partyId);
            assertThat(document.getPcsCase()).isEqualTo(caseEntity);
        });
        verify(accessCodeActivityLogService).logSuccess(caseEntity, saved.get(0).getParty(),
                                                   ClaimActivityType.DOCUMENTS_CREATED);
        verify(accessCodeActivityLogService, never()).logFailure(any(), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({"true,false,false", "false,true,true"})
    void generateForDefendant_welshRenderFails_savesNothingAndNamesTheWelshLetter(boolean firstAttempt,
                                                                                  boolean finalAttempt,
                                                                                  boolean terminal) {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        DocAssemblyException failure = new DocAssemblyException("Document generation failed");
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), eq(ENGLISH), any()))
            .thenReturn(DOC_URL);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), eq(WELSH), any()))
            .thenThrow(failure);

        DocAssemblyException thrown = assertThrows(DocAssemblyException.class,
            () -> underTest.generateForDefendant(5L, partyId, firstAttempt, finalAttempt));

        assertThat(thrown).isSameAs(failure);
        verify(documentRepository, never()).save(any());
        verify(partyAccessCodeRepo, never()).save(any());
        verify(testAccessCodeRecorder, never()).record(any(), any(), anyString());
        verify(accessCodeActivityLogService, never()).logSuccess(any(), any(), any());
        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED), detailsCaptor.capture());
        GenerationDetails details = detailsCaptor.getValue();
        assertThat(details.documentType()).isEqualTo(DocumentType.DEFENDANT_ACCESS_CODE);
        assertThat(details.failureReason()).isEqualTo(FailureReason.RENDER_FAILED);
        assertThat(details.terminal()).isEqualTo(terminal);
        assertThat(details.errorDetail()).isEqualTo("Welsh letter: DocAssemblyException: Document generation failed");
    }

    @Test
    void generateForDefendant_englishRenderFailsOnWalesCase_neverRendersTheWelshLetter() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), eq(ENGLISH), any()))
            .thenThrow(new RuntimeException("docmosis down"));

        assertThrows(RuntimeException.class, () -> underTest.generateForDefendant(5L, partyId, true, false));

        verify(accessCodeFormDocumentGenerator, never()).generate(any(), any(), any(), anyString(), eq(WELSH), any());
        verify(documentRepository, never()).save(any());
        verify(partyAccessCodeRepo, never()).save(any());
        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue().errorDetail()).isEqualTo("English letter: RuntimeException: docmosis down");
    }

    @Test
    void generateForDefendant_englandRenderFailure_keepsTheErrorDetailUnchanged() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("docmosis down"));

        assertThrows(RuntimeException.class, () -> underTest.generateForDefendant(5L, partyId, true, false));

        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue().errorDetail()).isEqualTo("RuntimeException: docmosis down");
        assertThat(detailsCaptor.getValue().failureReason()).isEqualTo(FailureReason.UNKNOWN);
    }

    @Test
    void generateForDefendant_saveFailsAfterBothRenders_doesNotBlameALanguage() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        stubBothLetters();
        when(documentRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> underTest.generateForDefendant(5L, partyId, true, false));

        verify(partyAccessCodeRepo, never()).save(any());
        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue().errorDetail()).isEqualTo("IllegalStateException: db down");
    }

    @Test
    void generateForDefendant_twoWalesDefendants_eachGetsTheirOwnCodeOnBothLetters() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(p1, p2);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        when(accessCodeGenerator.generateAccessCode()).thenReturn("AAAAAAAAAAAA", "BBBBBBBBBBBB");
        stubBothLetters();

        underTest.generateForDefendant(1L, p1, false, true);
        underTest.generateForDefendant(1L, p2, false, true);

        verify(accessCodeFormDocumentGenerator).generate(any(), any(), argThat(party -> p1.equals(party.getId())),
                                                         eq("AAAAAAAAAAAA"), eq(ENGLISH), any());
        verify(accessCodeFormDocumentGenerator).generate(any(), any(), argThat(party -> p1.equals(party.getId())),
                                                         eq("AAAAAAAAAAAA"), eq(WELSH), any());
        verify(accessCodeFormDocumentGenerator).generate(any(), any(), argThat(party -> p2.equals(party.getId())),
                                                         eq("BBBBBBBBBBBB"), eq(ENGLISH), any());
        verify(accessCodeFormDocumentGenerator).generate(any(), any(), argThat(party -> p2.equals(party.getId())),
                                                         eq("BBBBBBBBBBBB"), eq(WELSH), any());
        verify(documentRepository, times(4)).save(any());
        verify(partyAccessCodeRepo, times(2)).save(accessCodeCaptor.capture());
        assertThat(accessCodeCaptor.getAllValues())
            .extracting(PartyAccessCodeEntity::getCode)
            .containsExactly("ENC-AAAAAAAAAAAA", "ENC-BBBBBBBBBBBB");
        verify(testAccessCodeRecorder).record(caseEntity.getId(), p1, "AAAAAAAAAAAA");
        verify(testAccessCodeRecorder).record(caseEntity.getId(), p2, "BBBBBBBBBBBB");
        verify(testAccessCodeRecorder, times(2)).record(any(), any(), anyString());
    }

    @Test
    void generateForDefendant_walesCaseWithExistingCode_rendersNothing() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        when(partyAccessCodeRepo.existsByPcsCase_IdAndPartyId(caseEntity.getId(), partyId)).thenReturn(true);

        underTest.generateForDefendant(1L, partyId, false, true);

        verifyNoInteractions(accessCodeFormDocumentGenerator);
        verify(documentRepository, never()).save(any());
        verify(partyAccessCodeRepo, never()).save(any());
    }

    @Test
    void generateForDefendant_walesCase_resolvesTheCourtAndDateOnceForBothLetters() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        LetterContext letterContext = new LetterContext(null, LocalDate.of(2026, 11, 6));
        when(accessCodeFormDocumentGenerator.resolveLetterContext(caseEntity)).thenReturn(letterContext);
        stubBothLetters();

        underTest.generateForDefendant(1L, partyId, false, true);

        verify(accessCodeFormDocumentGenerator, times(1)).resolveLetterContext(any());
        verify(accessCodeFormDocumentGenerator)
            .generate(any(), any(), any(), anyString(), eq(ENGLISH), same(letterContext));
        verify(accessCodeFormDocumentGenerator)
            .generate(any(), any(), any(), anyString(), eq(WELSH), same(letterContext));
    }

    @Test
    void generateForDefendant_englandCase_resolvesTheContextOnceForItsOneLetter() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        caseEntity.setLegislativeCountry(LegislativeCountry.ENGLAND);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);
        LetterContext letterContext = new LetterContext(null, LocalDate.of(2026, 11, 6));
        when(accessCodeFormDocumentGenerator.resolveLetterContext(caseEntity)).thenReturn(letterContext);

        underTest.generateForDefendant(1L, partyId, false, true);

        verify(accessCodeFormDocumentGenerator, times(1)).resolveLetterContext(any());
        verify(accessCodeFormDocumentGenerator)
            .generate(any(), any(), any(), anyString(), eq(ENGLISH), same(letterContext));
        verify(accessCodeFormDocumentGenerator, never())
            .generate(any(), any(), any(), anyString(), eq(WELSH), any());
    }

    @Test
    void generateForDefendant_courtLookupFailsOnWalesCase_rendersNothingAndBlamesNoLanguage() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = walesCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(accessCodeFormDocumentGenerator.resolveLetterContext(caseEntity)).thenThrow(new IllegalStateException(
            "No case management location set for case 1; cannot resolve respond-by-post court"));

        assertThrows(IllegalStateException.class, () -> underTest.generateForDefendant(5L, partyId, true, false));

        verify(accessCodeFormDocumentGenerator, never()).generate(any(), any(), any(), anyString(), any(), any());
        verify(documentRepository, never()).save(any());
        verify(partyAccessCodeRepo, never()).save(any());
        verify(accessCodeActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue().failureReason()).isEqualTo(FailureReason.NO_COURT_LOCATION);
        assertThat(detailsCaptor.getValue().errorDetail()).isEqualTo(
            "IllegalStateException: No case management location set for case 1; cannot resolve respond-by-post court");
    }

    @Test
    void findDefendantPartyIdsNeedingAccessCode_skipsDefendantsThatAlreadyHaveCode() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(p1, p2);
        PartyAccessCodeEntity existing = mock(PartyAccessCodeEntity.class);
        when(existing.getPartyId()).thenReturn(p1);
        when(pcsCaseService.loadCase(2L)).thenReturn(caseEntity);
        when(partyAccessCodeRepo.findAllByPcsCase_Id(caseEntity.getId())).thenReturn(List.of(existing));

        List<UUID> result = underTest.findDefendantPartyIdsNeedingAccessCode(2L);

        assertThat(result).containsExactly(p2);
    }

    @Test
    void findDefendantPartyIdsNeedingAccessCode_skipsRemovedDefendants() {
        UUID activePartyId = UUID.randomUUID();
        UUID removedPartyId = UUID.randomUUID();
        PartyEntity activeParty = PartyEntity.builder().id(activePartyId).build();
        PartyEntity removedParty = PartyEntity.builder().id(removedPartyId).removed(true).build();
        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(activeParty).role(PartyRole.DEFENDANT).build(),
                ClaimPartyEntity.builder().party(removedParty).role(PartyRole.DEFENDANT).build()
            ))
            .build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(1234567890123456L)
            .claims(List.of(mainClaim))
            .build();
        when(pcsCaseService.loadCase(2L)).thenReturn(caseEntity);
        when(partyAccessCodeRepo.findAllByPcsCase_Id(caseEntity.getId())).thenReturn(List.of());

        List<UUID> result = underTest.findDefendantPartyIdsNeedingAccessCode(2L);

        assertThat(result).containsExactly(activePartyId);
    }

    private void stubBothLetters() {
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), eq(ENGLISH), any()))
            .thenReturn(DOC_URL);
        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), eq(WELSH), any()))
            .thenReturn(WELSH_DOC_URL);
    }

    private static PcsCaseEntity walesCaseWithDefendants(UUID... partyIds) {
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyIds);
        caseEntity.setLegislativeCountry(LegislativeCountry.WALES);
        return caseEntity;
    }

    private static PcsCaseEntity createCaseWithDefendants(UUID... partyIds) {
        List<ClaimPartyEntity> claimPartyList = Arrays.stream(partyIds)
            .map(partyId -> PartyEntity.builder().id(partyId).build())
            .map(party -> ClaimPartyEntity.builder().party(party).role(PartyRole.DEFENDANT).build())
            .toList();

        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(claimPartyList)
            .build();

        return PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(1234567890123456L)
            .claims(List.of(mainClaim))
            .build();
    }
}
