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
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimPackSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @Spy
    private SentPackDocuments sentPackDocuments = new SentPackDocuments(new ObjectMapper());

    @InjectMocks
    private ClaimPackSelector underTest;

    private final PartyEntity claimant = party();
    private final PartyEntity defendantA = party();
    private final PartyEntity defendantB = party();
    private final PartyEntity defendantC = party();
    private final DocumentEntity claimForm = document(DocumentType.CLAIM, null);

    @Test
    @DisplayName("Returns nothing when there is no claim form")
    void shouldReturnNothingWhenNoClaimForm() {
        PcsCaseEntity pcsCase = caseWith(null, List.of(), List.of(claimParty(claimant, PartyRole.CLAIMANT, 1)));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Includes claimant and a defendant whose access code is ready")
    void shouldIncludeClaimantAndReadyDefendant() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1)));

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().recipientType()).isEqualTo(PartyRole.CLAIMANT);
        assertThat(result.getFirst().documents()).containsExactly(claimForm);
        assertThat(result.get(1).recipientType()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(result.get(1).party()).isEqualTo(defendantA);
        assertThat(result.get(1).documents()).containsExactly(claimForm, pinA);
    }

    @Test
    @DisplayName("Excludes a claimant already sent the claim form")
    void shouldExcludeClaimantAlreadySent() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID))
            .thenReturn(List.of(sent(claimant, claimForm)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(), List.of(claimParty(claimant, PartyRole.CLAIMANT, 1)));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Re-sends only the access code when the claim form already reached the defendant")
    void shouldResendOnlyAccessCodeWhenClaimFormAlreadySent() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID))
            .thenReturn(List.of(sent(claimant, claimForm), sent(defendantA, claimForm)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1)));

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().party()).isEqualTo(defendantA);
        assertThat(result.getFirst().documents()).containsExactly(pinA);   // claim form not re-sent
    }

    @Test
    @DisplayName("Sends to the ready defendant and holds the one whose access code is missing")
    void shouldHandleMultipleDefendantsIndependently() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity pinC = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantC);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(claimant, claimForm),
            sent(defendantC, claimForm), sent(defendantC, pinC)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA, pinC), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1),   // pin, unsent  → included (ready)
            claimParty(defendantB, PartyRole.DEFENDANT, 2),   // no pin       → held
            claimParty(defendantC, PartyRole.DEFENDANT, 3))); // pin, sent    → excluded

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().party()).isEqualTo(defendantA);
        assertThat(result.getFirst().documents()).containsExactly(claimForm, pinA);
    }

    private PcsCaseEntity caseWith(DocumentEntity claimFormDocument, List<DocumentEntity> documents,
                                   List<ClaimPartyEntity> claimParties) {
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(claimParties)
            .claimFormDocument(claimFormDocument)
            .build();
        return PcsCaseEntity.builder().id(CASE_ID).claims(List.of(claim)).documents(documents).build();
    }

    private ClaimPartyEntity claimParty(PartyEntity party, PartyRole role, int rank) {
        return ClaimPartyEntity.builder().party(party).role(role).rank(rank).build();
    }

    private ClaimActivityLogEntity sent(PartyEntity party, DocumentEntity document) {
        try {
            String details = new ObjectMapper().writeValueAsString(PackDetails.sent(
                LetterType.CLAIMANT_CLAIM_PACK, List.of(new PackDocumentRef(document.getId(), document.getType())),
                UUID.randomUUID()));
            return ClaimActivityLogEntity.builder()
                .party(party).details(details)
                .activityType(ClaimActivityType.PACK_SENT).status(ClaimActivityStatus.SUCCESS).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }

    private DocumentEntity document(DocumentType type, PartyEntity party) {
        return DocumentEntity.builder().id(UUID.randomUUID()).type(type).party(party).build();
    }
}
