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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDocumentRef;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

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

    @Mock
    private FeatureToggleService featureToggleService;

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
    @DisplayName("When the rollout flag is off, sends the counterclaim to all parties even without a defence form")
    void shouldSendCounterClaimToAllPartiesWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result =
            underTest.findDefencePackCandidates(caseWith(List.of(counterClaim), claimant, defendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(counterClaim);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(counterClaim);
    }

    @Test
    @DisplayName("When the rollout flag is off, serves the defence form on every party, including a co-defendant")
    void shouldServeDefenceOnAllPartiesWhenRolloutFlagOff() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);

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
    @DisplayName("When the rollout flag is off, bundles defence and counter-claim for defendant and claimant")
    void shouldBundleDefenceAndCounterClaimForAllPartiesWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm, counterClaim), claimant, defendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(defenceForm, counterClaim);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(defenceForm, counterClaim);
    }

    @Test
    @DisplayName("When the rollout flag is off, serves defence and counter-claim on every party")
    void shouldServeDefenceAndCounterClaimOnAllPartiesWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
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
    @DisplayName("Sends a defence-only pack only to postal defendants and never to the claimant")
    void shouldSendDefenceOnlyToPostalDefendants() {
        PartyEntity postalClaimant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity postalDefendant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity nonPostalDefendant = partyWithPostPreference(VerticalYesNo.NO);
        DocumentEntity postalDefence = defenceForm(postalDefendant);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(postalDefence), postalClaimant, postalDefendant, nonPostalDefendant));

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.recipient()).isEqualTo(postalDefendant);
            assertThat(candidate.role()).isEqualTo(PartyRole.DEFENDANT);
            assertThat(candidate.documents()).containsExactly(postalDefence);
        });
    }

    @Test
    @DisplayName("Excludes missing, null and non-postal contact preferences")
    void shouldExcludeDefendantsWithoutExplicitPostPreference() {
        PartyEntity postalDefendant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity nonPostalDefendant = partyWithPostPreference(VerticalYesNo.NO);
        PartyEntity missingPreferencesDefendant = party();
        PartyEntity nullPostPreferenceDefendant = partyWithPostPreference(null);
        DocumentEntity postalDefence = defenceForm(postalDefendant);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(postalDefence), claimant, postalDefendant, nonPostalDefendant,
                missingPreferencesDefendant, nullPostPreferenceDefendant));

        assertThat(result).extracting(candidate -> candidate.recipient().getId())
            .containsExactly(postalDefendant.getId());
    }

    @Test
    @DisplayName("Bundles defence and issued counterclaim only for postal defendants")
    void shouldBundleDefenceAndCounterClaimForPostalDefendants() {
        PartyEntity postalClaimant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity postalDefendant = partyWithPostPreference(VerticalYesNo.YES);
        DocumentEntity postalDefence = defenceForm(postalDefendant);
        DocumentEntity postalCounterClaim = counterClaim(postalDefendant);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(postalDefence, postalCounterClaim), postalClaimant, postalDefendant));

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.recipient()).isEqualTo(postalDefendant);
            assertThat(candidate.documents()).containsExactly(postalDefence, postalCounterClaim);
        });
    }

    @Test
    @DisplayName("Sends a counterclaim before its defence is generated only to postal defendants")
    void shouldSendCounterClaimOnlyToPostalDefendants() {
        PartyEntity postalClaimant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity respondingDefendant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity postalCoDefendant = partyWithPostPreference(VerticalYesNo.YES);
        DocumentEntity issuedCounterClaim = counterClaim(respondingDefendant);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(issuedCounterClaim), postalClaimant, respondingDefendant, postalCoDefendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, respondingDefendant).documents()).containsExactly(issuedCounterClaim);
        assertThat(candidateFor(result, postalCoDefendant).documents()).containsExactly(issuedCounterClaim);
    }

    @Test
    @DisplayName("Serves late counterclaims to every postal co-defendant without resending defences")
    void shouldSendOnlyLateCounterClaimToPostalCoDefendants() {
        PartyEntity respondingDefendant = partyWithPostPreference(VerticalYesNo.YES);
        PartyEntity postalCoDefendant = partyWithPostPreference(VerticalYesNo.YES);
        DocumentEntity response = defenceForm(respondingDefendant);
        DocumentEntity issuedCounterClaim = counterClaim(respondingDefendant);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(
            List.of(sent(respondingDefendant, response), sent(postalCoDefendant, response)));

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(response, issuedCounterClaim), claimant, respondingDefendant, postalCoDefendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, respondingDefendant).documents()).containsExactly(issuedCounterClaim);
        assertThat(candidateFor(result, postalCoDefendant).documents()).containsExactly(issuedCounterClaim);
    }

    @Test
    @DisplayName("When the rollout flag is off, sends only the unsent counter-claim after defence was posted")
    void shouldSendOnlyUnsentCounterClaimLaterWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(defendant, defenceForm), sent(claimant, defenceForm)));

        List<DefencePackCandidate> result = underTest.findDefencePackCandidates(
            caseWith(List.of(defenceForm, counterClaim), claimant, defendant));

        assertThat(result).hasSize(2);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(counterClaim);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(counterClaim);
    }

    @Test
    @DisplayName("When the rollout flag is off, returns nothing once every party has every document")
    void shouldReturnNothingWhenAllSentWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
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
                LetterType.DEFENCE_PACK,
                List.of(new PackDocumentRef(document.getId(), document.getType(), null, false)),
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

    private PartyEntity partyWithPostPreference(VerticalYesNo contactByPost) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .contactPreferences(ContactPreferencesEntity.builder().contactByPost(contactByPost).build())
            .build();
    }
}
