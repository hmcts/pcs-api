package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingNoticeSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    private HearingNoticeSelector underTest;

    private final PartyEntity claimant = party();
    private final PartyEntity defendant = party();
    private final HearingEntity hearing = hearing(VerticalYesNo.YES, null);

    @BeforeEach
    void setUp() {
        underTest = new HearingNoticeSelector(claimActivityLogRepository, new SentPackDocuments(new ObjectMapper()));
        lenient().when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("Posts an unrepresented defendant's notice to that defendant")
    void shouldPostDefendantNotice() {
        DocumentEntity notice = notice(hearing, defendant);

        assertThat(select(notice)).singleElement().satisfies(candidate -> {
            assertThat(candidate.role()).isEqualTo(PartyRole.DEFENDANT);
            assertThat(candidate.recipient()).isEqualTo(defendant);
            assertThat(candidate.documents()).containsExactly(notice);
        });
    }

    @Test
    @DisplayName("Never posts a claimant's notice")
    void shouldNotPostClaimantNotice() {
        assertThat(select(notice(hearing, claimant))).isEmpty();
    }

    @Test
    @DisplayName("Skips a represented defendant")
    void shouldSkipRepresentedDefendant() {
        defendant.getClaimPartyOrganisationList().add(ClaimPartyOrganisationEntity.builder()
            .active(YesOrNo.YES).build());

        assertThat(select(notice(hearing, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Skips a defendant who opted out of post")
    void shouldSkipDefendantOptedOutOfPost() {
        defendant.setContactPreferences(ContactPreferencesEntity.builder().contactByPost(VerticalYesNo.NO).build());

        assertThat(select(notice(hearing, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Skips a notice already covered by PACK_SENT")
    void shouldSkipAlreadySentNotice() {
        DocumentEntity notice = notice(hearing, defendant);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(sent(defendant, notice)));

        assertThat(select(notice)).isEmpty();
    }

    @Test
    @DisplayName("Skips notices for cancelled hearings or hearings that no longer issue a notice")
    void shouldSkipCancelledOrNotIssuedHearings() {
        assertThat(select(
            notice(hearing(VerticalYesNo.YES, true), defendant),
            notice(hearing(VerticalYesNo.NO, null), defendant))).isEmpty();
    }

    @Test
    @DisplayName("Ignores uploaded notices of hearing")
    void shouldIgnoreUploadedNotices() {
        assertThat(select(DocumentEntity.builder().id(UUID.randomUUID())
            .type(DocumentType.NOTICE_OF_HEARING).party(defendant).build())).isEmpty();
    }

    @Test
    @DisplayName("Returns nothing when the case has no claim")
    void shouldReturnNothingWithoutClaim() {
        assertThat(underTest.findHearingNoticePackCandidates(PcsCaseEntity.builder().id(CASE_ID).build())).isEmpty();
    }

    private List<HearingNoticePackCandidate> select(DocumentEntity... documents) {
        return underTest.findHearingNoticePackCandidates(caseWith(documents));
    }

    private PcsCaseEntity caseWith(DocumentEntity... documents) {
        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).rank(1).build()
        )).build();
        return PcsCaseEntity.builder().id(CASE_ID).claims(List.of(claim)).documents(List.of(documents)).build();
    }

    private static HearingEntity hearing(VerticalYesNo issueNotice, Boolean cancelled) {
        return HearingEntity.builder().issueNotice(issueNotice).cancelled(cancelled).build();
    }

    private static DocumentEntity notice(HearingEntity hearing, PartyEntity recipient) {
        return DocumentEntity.builder().id(UUID.randomUUID())
            .type(DocumentType.HEARING_NOTICE).hearing(hearing).party(recipient).build();
    }

    private ClaimActivityLogEntity sent(PartyEntity party, DocumentEntity document) {
        try {
            String details = new ObjectMapper().writeValueAsString(PackDetails.sent(
                LetterType.HEARING_NOTICE_PACK,
                List.of(new PackDocumentRef(document.getId(), document.getType(), null, false)),
                UUID.randomUUID()));
            return ClaimActivityLogEntity.builder()
                .party(party).details(details)
                .activityType(ClaimActivityType.PACK_SENT).status(ClaimActivityStatus.SUCCESS).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
