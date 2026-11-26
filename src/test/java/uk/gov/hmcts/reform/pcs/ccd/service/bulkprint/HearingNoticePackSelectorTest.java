package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
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
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class HearingNoticePackSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final LocalDateTime NEXT_MONTH = LocalDateTime.of(2026, 11, 30, 10, 0);

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;
    @Mock
    private FeatureToggleService featureToggleService;

    private final List<ClaimActivityLogEntity> activityLog = new ArrayList<>();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant = party();
    private final PartyEntity coDefendant = party();
    private ClaimEntity claim;
    private PcsCaseEntity pcsCase;
    private HearingNoticePackSelector underTest;

    @BeforeEach
    void setUp() {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);
        lenient().when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(activityLog);
        underTest = new HearingNoticePackSelector(claimActivityLogRepository,
            new SentPackDocuments(new ObjectMapper()),
            new PackSkipRules(featureToggleService, new CaseFlagService(null, null, null, null, null),
                Clock.fixed(NOW, ZoneId.of("UTC")), Duration.ofHours(24)),
            Clock.fixed(NOW, ZoneId.of("Europe/London")));

        claim = ClaimEntity.builder().claimParties(new ArrayList<>(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(coDefendant).role(PartyRole.DEFENDANT).rank(2).build()))).build();
        pcsCase = PcsCaseEntity.builder().id(CASE_ID).caseReference(1234567890123456L)
            .claims(List.of(claim)).build();
    }

    @Test
    void postsEachPartyTheirOwnNoticeAsASeparateLetter() {
        HearingEntity hearing = hearing(1, NEXT_MONTH, 1);
        DocumentEntity claimantNotice = notice(hearing, claimant, "Notice of hearing 1 - Claimant.pdf", 1);
        DocumentEntity defendantNotice = notice(hearing, defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);

        List<HearingNoticePackCandidate> candidates = underTest.findHearingNoticePackCandidates(pcsCase);

        assertThat(candidates).extracting(HearingNoticePackCandidate::recipient)
            .containsExactlyInAnyOrder(claimant, defendant);
        assertThat(candidates).filteredOn(candidate -> candidate.recipient() == defendant).singleElement()
            .satisfies(candidate -> {
                assertThat(candidate.role()).isEqualTo(PartyRole.DEFENDANT);
                assertThat(candidate.documents()).containsExactly(defendantNotice);
            });
        assertThat(candidates).filteredOn(candidate -> candidate.recipient() == claimant).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(claimantNotice));
    }

    @Test
    void neverPostsTheSameNoticeTwice() {
        HearingEntity hearing = hearing(1, NEXT_MONTH, 1);
        DocumentEntity posted = notice(hearing, defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);
        activityLog.add(sent(defendant, posted));

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).isEmpty();
    }

    @Test
    void postsOnlyTheNoticeForTheHearingsCurrentNoticeNumber() {
        HearingEntity hearing = hearing(1, NEXT_MONTH, 2);
        notice(hearing, defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);

        // Edited after the first notice: nothing is posted until the replacement notice has been generated.
        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).isEmpty();

        DocumentEntity replacement = notice(hearing, defendant, "Notice of hearing 2 - Defendant 1.pdf", 2);
        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(replacement));
    }

    @Test
    void postsTheLatestNoticeWhenANoticeNumberWasGeneratedMoreThanOnce() {
        HearingEntity hearing = hearing(1, NEXT_MONTH, 1);
        notice(hearing, defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);
        DocumentEntity regenerated = notice(hearing, defendant, "Notice of hearing 1 - Defendant 1.pdf", 2);

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(regenerated));
    }

    @Test
    void doesNotPostNoticesForHearingsThatNoLongerNeedOne() {
        HearingEntity cancelled = hearing(1, NEXT_MONTH, 1);
        cancelled.setCancelled(true);
        HearingEntity past = hearing(2, LocalDateTime.of(2026, 10, 29, 11, 59), 2);
        HearingEntity noNotice = hearing(3, NEXT_MONTH, 3);
        noNotice.setIssueNotice(VerticalYesNo.NO);
        final HearingEntity undated = hearing(4, null, 4);
        notice(cancelled, defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);
        notice(past, defendant, "Notice of hearing 2 - Defendant 1.pdf", 1);
        notice(noNotice, defendant, "Notice of hearing 3 - Defendant 1.pdf", 1);
        notice(undated, defendant, "Notice of hearing 4 - Defendant 1.pdf", 1);

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).isEmpty();
    }

    @Test
    void postsNothingWhoseNoticeNumberCannotBeRead() {
        HearingEntity hearing = hearing(1, NEXT_MONTH, 1);
        notice(hearing, defendant, "Renamed notice.pdf", 1);
        notice(hearing, coDefendant, null, 1);

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).isEmpty();
    }

    @Test
    void leavesOutPartiesServedOtherwiseOrNoLongerOnTheClaim() {
        final HearingEntity hearing = hearing(1, NEXT_MONTH, 1);
        PartyEntity represented = addDefendant(3);
        represented.setClaimPartyOrganisationList(List.of(
            ClaimPartyOrganisationEntity.builder().active(YesOrNo.YES).build()));
        PartyEntity noPost = addDefendant(4);
        noPost.setContactPreferences(ContactPreferencesEntity.builder().contactByPost(VerticalYesNo.NO).build());
        PartyEntity removed = addDefendant(5);
        removed.setRemoved(true);
        final PartyEntity stranger = party();
        notice(hearing, represented, "Notice of hearing 1 - Defendant 3.pdf", 1);
        notice(hearing, noPost, "Notice of hearing 1 - Defendant 4.pdf", 1);
        notice(hearing, removed, "Notice of hearing 1 - Defendant 5.pdf", 1);
        notice(hearing, stranger, "Notice of hearing 1 - Someone.pdf", 1);
        DocumentEntity postable = notice(hearing, coDefendant, "Notice of hearing 1 - Defendant 2.pdf", 1);

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(postable));
    }

    @Test
    void leavesOutClaimantsWhenTheyAreExcludedFromEveryPack() {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
        HearingEntity hearing = hearing(1, NEXT_MONTH, 1);
        notice(hearing, claimant, "Notice of hearing 1 - Claimant.pdf", 1);
        notice(hearing, defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase))
            .extracting(HearingNoticePackCandidate::recipient).containsExactly(defendant);
    }

    @Test
    void holdsBackNoticesThatNeedTranslation() {
        claim.setLanguageUsed(LanguageUsed.WELSH);
        notice(hearing(1, NEXT_MONTH, 1), defendant, "Notice of hearing 1 - Defendant 1.pdf", 1);

        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).isEmpty();
    }

    @Test
    void ignoresOtherDocumentsAndCasesWithoutAClaim() {
        DocumentEntity other = notice(hearing(1, NEXT_MONTH, 1), defendant, "Notice of hearing 1.pdf", 1);
        other.setType(DocumentType.GENERAL_APPLICATION);
        assertThat(underTest.findHearingNoticePackCandidates(pcsCase)).isEmpty();

        assertThat(underTest.findHearingNoticePackCandidates(
            PcsCaseEntity.builder().id(CASE_ID).claims(List.of()).build())).isEmpty();
    }

    private PartyEntity addDefendant(int rank) {
        PartyEntity added = party();
        claim.getClaimParties().add(
            ClaimPartyEntity.builder().party(added).role(PartyRole.DEFENDANT).rank(rank).build());
        return added;
    }

    private HearingEntity hearing(int id, LocalDateTime date, int noticeNumber) {
        return HearingEntity.builder().id(id).hearingDate(date).issueNotice(VerticalYesNo.YES)
            .noticeNumber(noticeNumber).build();
    }

    private DocumentEntity notice(HearingEntity hearing, PartyEntity party, String fileName, int createdSecond) {
        DocumentEntity notice = DocumentEntity.builder().id(UUID.randomUUID()).type(DocumentType.NOTICE_OF_HEARING)
            .party(party).hearing(hearing).fileName(fileName).submittedDate(NOW.minusSeconds(100 - createdSecond))
            .build();
        pcsCase.getDocuments().add(notice);
        return notice;
    }

    private static ClaimActivityLogEntity sent(PartyEntity party, DocumentEntity document) {
        try {
            String details = new ObjectMapper().writeValueAsString(PackDetails.sent(LetterType.HEARING_NOTICE_PACK,
                List.of(new PackDocumentRef(document.getId(), document.getType(), null, true)), UUID.randomUUID()));
            return ClaimActivityLogEntity.builder().party(party).details(details)
                .activityType(ClaimActivityType.PACK_SENT).status(ClaimActivityStatus.SUCCESS).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
