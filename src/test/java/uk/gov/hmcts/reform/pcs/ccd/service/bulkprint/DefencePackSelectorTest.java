package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefencePackSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final long CASE_REF = 1234567890123456L;

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;
    @Mock
    private CounterClaimRepository counterClaimRepository;

    @InjectMocks
    private DefencePackSelector underTest;

    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity defenceForm = defenceFormDocument();
    private final DocumentEntity counterClaimDoc = document(DocumentType.COUNTERCLAIM);

    @BeforeEach
    void noActivityOrCounterClaimByDefault() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdOrderByClaimSubmittedDateDesc(
            CASE_REF, defendant.getId())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Defence only, complete, when there is no counter-claim")
    void shouldSelectDefenceOnlyAsCompleteWhenNoCounterClaim() {
        DefencePackCandidate candidate = only(caseWith(List.of(defenceForm)));

        assertThat(candidate.documents()).containsExactly(defenceForm);
        assertThat(candidate.targetStatus()).isEqualTo(ClaimActivityType.DEFENCE_PACK_SENT);
    }

    @Test
    @DisplayName("Defence only, partial, when the counter-claim is pending")
    void shouldSelectDefenceAsPartialWhenCounterClaimPending() {
        stubCounterClaim(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);

        DefencePackCandidate candidate = only(caseWith(List.of(defenceForm)));

        assertThat(candidate.documents()).containsExactly(defenceForm);
        assertThat(candidate.targetStatus()).isEqualTo(ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT);
    }

    @Test
    @DisplayName("Bundles defence and counter-claim when both are ready")
    void shouldBundleBothAsCompleteWhenCounterClaimIssuedAndDocReady() {
        stubCounterClaim(CounterClaimState.COUNTER_CLAIM_ISSUED);

        DefencePackCandidate candidate = only(caseWith(List.of(defenceForm, counterClaimDoc)));

        assertThat(candidate.documents()).containsExactly(defenceForm, counterClaimDoc);
        assertThat(candidate.targetStatus()).isEqualTo(ClaimActivityType.DEFENCE_PACK_SENT);
    }

    @Test
    @DisplayName("Completes a partial with the counter-claim when later issued")
    void shouldCompletePartialWithCounterClaimWhenLaterIssued() {
        stubActivity(ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT);
        stubCounterClaim(CounterClaimState.COUNTER_CLAIM_ISSUED);

        DefencePackCandidate candidate = only(caseWith(List.of(defenceForm, counterClaimDoc)));

        assertThat(candidate.documents()).containsExactly(counterClaimDoc);   // second envelope
        assertThat(candidate.targetStatus()).isEqualTo(ClaimActivityType.DEFENCE_PACK_SENT);
    }

    @Test
    @DisplayName("Stays partial while the counter-claim is still pending")
    void shouldStayPartialWhileCounterClaimStillPending() {
        stubActivity(ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT);
        stubCounterClaim(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);

        assertThat(underTest.findDefencePackCandidates(caseWith(List.of(defenceForm)))).isEmpty();
    }

    @Test
    @DisplayName("Returns nothing when already sent")
    void shouldReturnNothingWhenAlreadySent() {
        stubActivity(ClaimActivityType.DEFENCE_PACK_SENT);

        assertThat(underTest.findDefencePackCandidates(caseWith(List.of(defenceForm)))).isEmpty();
    }

    private DefencePackCandidate only(PcsCaseEntity pcsCase) {
        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(pcsCase);
        assertThat(result).hasSize(1);
        return result.getFirst();
    }

    private void stubCounterClaim(CounterClaimState state) {
        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdOrderByClaimSubmittedDateDesc(
            CASE_REF, defendant.getId()))
            .thenReturn(Optional.of(CounterClaimEntity.builder().party(defendant).status(state).build()));
    }

    private void stubActivity(ClaimActivityType activityType) {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            ClaimActivityLogEntity.builder()
                .party(defendant).activityType(activityType).status(ClaimActivityStatus.SUCCESS).build()));
    }

    private PcsCaseEntity caseWith(List<DocumentEntity> documents) {
        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .party(defendant).role(PartyRole.DEFENDANT).rank(1).build();
        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(claimParty)).build();
        return PcsCaseEntity.builder()
            .id(CASE_ID).caseReference(CASE_REF).claims(List.of(claim)).documents(documents).build();
    }

    private DocumentEntity defenceFormDocument() {
        return DocumentEntity.builder()
            .documentId(UUID.randomUUID())
            .type(DocumentType.DEFENDANT_RESPONSE)
            .defendantResponse(DefendantResponseEntity.builder().party(defendant).build())
            .build();
    }

    private DocumentEntity document(DocumentType type) {
        return DocumentEntity.builder().documentId(UUID.randomUUID()).type(type).party(defendant).build();
    }
}
