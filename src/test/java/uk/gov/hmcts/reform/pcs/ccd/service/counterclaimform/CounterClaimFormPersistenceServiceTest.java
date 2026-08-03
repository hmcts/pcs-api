package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimFormPersistenceServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private CounterClaimFormPayloadBuilder payloadBuilder;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @InjectMocks
    private CounterClaimFormPersistenceService underTest;

    @Test
    void buildsContextWithPayloadAndDefendantNumberWhenNotAttached() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        CounterClaimEntity counterClaim = counterClaimFor(defendant, 2);
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));
        when(documentRepository.existsByCounterClaim_IdAndType(COUNTER_CLAIM_ID, DocumentType.COUNTERCLAIM))
            .thenReturn(false);
        when(payloadBuilder.build(counterClaim)).thenReturn(payload);

        Optional<CounterClaimFormRenderContext> context = underTest.buildContextIfNotAttached(COUNTER_CLAIM_ID);

        assertThat(context).isPresent();
        assertThat(context.get().payload()).isSameAs(payload);
        assertThat(context.get().defendantNumber()).isEqualTo(2);
    }

    @Test
    void returnsEmptyAndDoesNotBuildWhenAlreadyAttached() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        CounterClaimEntity counterClaim = counterClaimFor(defendant, 1);
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));
        when(documentRepository.existsByCounterClaim_IdAndType(COUNTER_CLAIM_ID, DocumentType.COUNTERCLAIM))
            .thenReturn(true);

        assertThat(underTest.buildContextIfNotAttached(COUNTER_CLAIM_ID)).isEmpty();
        verifyNoInteractions(payloadBuilder);
    }

    @Test
    void defendantNumberFallsBackToOneWhenRankMissing() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        CounterClaimEntity counterClaim = counterClaimFor(defendant, null);
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));
        when(documentRepository.existsByCounterClaim_IdAndType(COUNTER_CLAIM_ID, DocumentType.COUNTERCLAIM))
            .thenReturn(false);
        when(payloadBuilder.build(counterClaim)).thenReturn(CounterClaimFormPayload.builder().build());

        Optional<CounterClaimFormRenderContext> context = underTest.buildContextIfNotAttached(COUNTER_CLAIM_ID);

        assertThat(context).isPresent();
        assertThat(context.get().defendantNumber()).isOne();
    }

    @Test
    void attachStoresTypeLinksDocumentAndLogsSuccess() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        CounterClaimEntity counterClaim = counterClaimFor(defendant, 1);
        PcsCaseEntity pcsCase = counterClaim.getPcsCase();
        DocumentEntity document = DocumentEntity.builder().build();
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));
        when(documentRepository.existsByCounterClaim_IdAndType(COUNTER_CLAIM_ID, DocumentType.COUNTERCLAIM))
            .thenReturn(false);
        when(documentImportService.addDocumentToCase(
            pcsCase, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE)).thenReturn(document);

        underTest.attach(COUNTER_CLAIM_ID, DM_STORE_URL);

        assertThat(document.getType()).isEqualTo(DocumentType.COUNTERCLAIM);
        assertThat(document.getCounterClaim()).isSameAs(counterClaim);
        assertThat(document.getParty()).isSameAs(defendant);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, defendant);
    }

    @Test
    void attachSkipsWhenAlreadyAttached() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        CounterClaimEntity counterClaim = counterClaimFor(defendant, 1);
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));
        when(documentRepository.existsByCounterClaim_IdAndType(COUNTER_CLAIM_ID, DocumentType.COUNTERCLAIM))
            .thenReturn(true);

        underTest.attach(COUNTER_CLAIM_ID, DM_STORE_URL);

        verify(documentImportService, never()).addDocumentToCase(
            any(PcsCaseEntity.class), anyString(), any(CaseFileCategory.class));
        verifyNoInteractions(claimActivityLogService);
    }

    @Test
    void recordGenerationFailureLogsAgainstFilingDefendantAndReturnsCaseReference() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        CounterClaimEntity counterClaim = counterClaimFor(defendant, 1);
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));

        long caseReference = underTest.recordGenerationFailure(COUNTER_CLAIM_ID, new RuntimeException("boom"), false);

        assertThat(caseReference).isEqualTo(CASE_REFERENCE);
        verify(claimActivityLogService).logGenerationFailure(eq(CASE_REFERENCE), eq(defendant.getId()),
            any(GenerationDetails.class));
    }

    @Test
    void throwsWhenCounterClaimNotFound() {
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.buildContextIfNotAttached(COUNTER_CLAIM_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(COUNTER_CLAIM_ID.toString());
    }

    private CounterClaimEntity counterClaimFor(PartyEntity defendant, Integer rank) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .party(defendant)
            .role(PartyRole.DEFENDANT)
            .rank(rank)
            .build();
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(new ArrayList<>(List.of(claimParty)))
            .build();
        pcsCase.setClaims(new ArrayList<>(List.of(claim)));
        return CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .pcsCase(pcsCase)
            .party(defendant)
            .build();
    }
}
