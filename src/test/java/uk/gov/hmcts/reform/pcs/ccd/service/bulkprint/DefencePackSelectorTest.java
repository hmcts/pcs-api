package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDocumentRef;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefencePackSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final long CASE_REF = 1234567890123456L;

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @Spy
    private SentPackDocuments sentPackDocuments = new SentPackDocuments(new ObjectMapper());

    @InjectMocks
    private DefencePackSelector underTest;

    private final PartyEntity claimant = party();
    private final PartyEntity defendant = party();
    private final PartyEntity coDefendant = party();
    private final DocumentEntity defenceForm = defenceForm(defendant);
    private final DocumentEntity counterClaim = counterClaim(defendant);

    @Test
    @DisplayName("Returns nothing when there is no defence form")
    void shouldReturnNothingWhenNoDefenceForm() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        assertThat(underTest.findDefencePackCandidates(caseWith(List.of(), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Serves the defence form on every party, including a co-defendant")
    void shouldServeDefenceOnAllParties() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm), claimant, defendant, coDefendant));

        assertThat(result).hasSize(3);
        assertThat(candidateFor(result, defendant).role()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(defenceForm);
        assertThat(candidateFor(result, claimant).role()).isEqualTo(PartyRole.CLAIMANT);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(defenceForm);
        assertThat(candidateFor(result, coDefendant).documents()).containsExactly(defenceForm);
    }

    @Test
    @DisplayName("Bundles the defence and counter-claim for the defendant and claimant when both are ready")
    void shouldBundleDefenceAndCounterClaimWhenBothReady() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm, counterClaim), claimant, defendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(defenceForm, counterClaim);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(defenceForm, counterClaim);
    }

    @Test
    @DisplayName("Serves the defence form and counter-claim on every party, including a co-defendant")
    void shouldServeDefenceAndCounterClaimOnAllParties() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm, counterClaim), claimant, defendant, coDefendant));

        assertThat(result).hasSize(3);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(defenceForm, counterClaim);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(defenceForm, counterClaim);
        assertThat(candidateFor(result, coDefendant).documents()).containsExactly(defenceForm, counterClaim);
        assertThat(candidateFor(result, coDefendant).role()).isEqualTo(PartyRole.DEFENDANT);
    }

    @Test
    @DisplayName("Sends only the counter-claim when the defence form has already been posted")
    void shouldSendOnlyUnsentCounterClaimLater() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(defendant, defenceForm), sent(claimant, defenceForm)));

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm, counterClaim), claimant, defendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(counterClaim);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(counterClaim);
    }

    @Test
    @DisplayName("Returns nothing when every document has already been sent to every recipient")
    void shouldReturnNothingWhenAllSent() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(defendant, defenceForm), sent(claimant, defenceForm),
            sent(defendant, counterClaim), sent(claimant, counterClaim)));

        assertThat(underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm, counterClaim), claimant, defendant))).isEmpty();
    }

    private DefencePackCandidate candidateFor(List<DefencePackCandidate> result, PartyEntity recipient) {
        return result.stream()
            .filter(candidate -> candidate.recipient().getId().equals(recipient.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no candidate for recipient " + recipient.getId()));
    }

    private PcsCaseEntity caseWith(List<DocumentEntity> documents, PartyEntity claimantParty,
                                   PartyEntity... defendantParties) {
        List<ClaimPartyEntity> claimParties = new ArrayList<>();
        claimParties.add(ClaimPartyEntity.builder().party(claimantParty).role(PartyRole.CLAIMANT).rank(1).build());
        int rank = 1;
        for (PartyEntity defendantParty : defendantParties) {
            claimParties.add(
                ClaimPartyEntity.builder().party(defendantParty).role(PartyRole.DEFENDANT).rank(rank++).build());
        }
        ClaimEntity claim = ClaimEntity.builder().claimParties(claimParties).build();
        return PcsCaseEntity.builder()
            .id(CASE_ID).caseReference(CASE_REF).claims(List.of(claim)).documents(documents).build();
    }

    private ClaimActivityLogEntity sent(PartyEntity party, DocumentEntity document) {
        try {
            String details = new ObjectMapper().writeValueAsString(PackDetails.sent(
                LetterType.DEFENCE_PACK, List.of(new PackDocumentRef(document.getId(), document.getType())),
                UUID.randomUUID()));
            return ClaimActivityLogEntity.builder()
                .party(party).details(details)
                .activityType(ClaimActivityType.PACK_SENT).status(ClaimActivityStatus.SUCCESS).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private DocumentEntity defenceForm(PartyEntity owner) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .type(DocumentType.DEFENDANT_RESPONSE)
            .defendantResponse(DefendantResponseEntity.builder().party(owner).build())
            .build();
    }

    private DocumentEntity counterClaim(PartyEntity owner) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID()).type(DocumentType.COUNTERCLAIM).party(owner).build();
    }

    private PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
