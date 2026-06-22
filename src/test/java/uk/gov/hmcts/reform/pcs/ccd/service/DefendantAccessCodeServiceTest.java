package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.PinPackDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.service.TestPinRecorder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantAccessCodeServiceTest {

    private static final String DOC_URL = "http://dm-store/documents/abc";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepo;
    @Mock(strictness = LENIENT)
    private AccessCodeGenerator accessCodeGenerator;
    @Mock
    private PartyAccessCodeHashingService hashingService;
    @Mock(strictness = LENIENT)
    private PinPackDocumentGenerator pinPackDocumentGenerator;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ClaimActivityLogService claimActivityLogService;
    @Mock
    private TestPinRecorder testPinRecorder;

    private DefendantAccessCodeService underTest;

    @Captor
    private ArgumentCaptor<PartyAccessCodeEntity> accessCodeCaptor;
    @Captor
    private ArgumentCaptor<DocumentEntity> documentCaptor;

    @BeforeEach
    void setUp() {
        underTest = new DefendantAccessCodeService(
            pcsCaseService,
            partyAccessCodeRepo,
            accessCodeGenerator,
            hashingService,
            pinPackDocumentGenerator,
            documentRepository,
            claimActivityLogService,
            testPinRecorder
        );
        when(accessCodeGenerator.generateAccessCode()).thenCallRealMethod();
        lenient().when(pinPackDocumentGenerator.generatePinPack(any(), any(), any(), anyString()))
            .thenReturn(DOC_URL);
        lenient().when(hashingService.encodeForStorage(anyString()))
            .thenAnswer(invocation -> "ENC-" + invocation.getArgument(0));
    }

    @Test
    void generateForDefendant_savesDocumentEncryptedCodePinAndLogsSuccess() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(1L)).thenReturn(caseEntity);

        underTest.generateForDefendant(1L, partyId, true);

        verify(documentRepository).save(documentCaptor.capture());
        DocumentEntity savedDoc = documentCaptor.getValue();
        assertThat(savedDoc.getType()).isEqualTo(DocumentType.DEFENDANT_ACCESS_CODE);
        assertThat(savedDoc.getCategoryId()).isNull();
        assertThat(savedDoc.getUrl()).isEqualTo(DOC_URL);
        assertThat(savedDoc.getParty().getId()).isEqualTo(partyId);
        assertThat(savedDoc.getPcsCase()).isEqualTo(caseEntity);

        verify(partyAccessCodeRepo).save(accessCodeCaptor.capture());
        PartyAccessCodeEntity savedCode = accessCodeCaptor.getValue();
        assertThat(savedCode.getPartyId()).isEqualTo(partyId);
        assertThat(savedCode.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(savedCode.getCode()).startsWith("ENC-");

        verify(testPinRecorder).record(eq(caseEntity.getId()), eq(partyId), anyString());
        verify(claimActivityLogService).logSuccess(caseEntity, savedDoc.getParty(),
                                                   ClaimActivityType.DOCUMENTS_CREATED);
        verify(claimActivityLogService, never()).logFailure(any(), any(), any());
    }

    @Test
    void generateForDefendant_finalAttemptFailure_logsFailureAndRethrows() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(pinPackDocumentGenerator.generatePinPack(any(), any(), any(), anyString()))
            .thenThrow(new RuntimeException("docmosis down"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> underTest.generateForDefendant(5L, partyId, true));
        assertThat(thrown).hasMessage("docmosis down");

        verify(claimActivityLogService).logFailure(eq(caseEntity), any(PartyEntity.class),
                                                   eq(ClaimActivityType.DOCUMENTS_CREATED));
        verify(partyAccessCodeRepo, never()).save(any());
        verify(documentRepository, never()).save(any());
        verify(testPinRecorder, never()).record(any(), any(), anyString());
    }

    @Test
    void generateForDefendant_nonFinalAttemptFailure_doesNotLogFailure() {
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseWithDefendants(partyId);
        when(pcsCaseService.loadCase(5L)).thenReturn(caseEntity);
        when(pinPackDocumentGenerator.generatePinPack(any(), any(), any(), anyString()))
            .thenThrow(new RuntimeException("docmosis down"));

        assertThrows(RuntimeException.class, () -> underTest.generateForDefendant(5L, partyId, false));

        verify(claimActivityLogService, never()).logFailure(any(), any(), any());
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
