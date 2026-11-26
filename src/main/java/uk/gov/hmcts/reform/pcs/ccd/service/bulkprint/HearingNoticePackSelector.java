package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;
import static uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingNoticePersistenceService.noticeNumberOf;

/**
 * Selects the notices of hearing to post. A notice is posted only while it is still the notice for its hearing:
 * the hearing still issues a notice, is not cancelled, has not taken place, and has not been given a later notice
 * number by an edit. Each notice goes to the party it was generated for, when that party is still on the claim,
 * accepts post and is not served through a legal representative. Claimants are left out when the release 1.4 flag
 * excludes them from every pack. A posted notice is never posted again.
 */
@Service
public class HearingNoticePackSelector {

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SentPackDocuments sentPackDocuments;
    private final PackSkipRules packSkipRules;
    private final Clock ukClock;

    public HearingNoticePackSelector(ClaimActivityLogRepository claimActivityLogRepository,
                                     SentPackDocuments sentPackDocuments,
                                     PackSkipRules packSkipRules,
                                     @Qualifier("ukClock") Clock ukClock) {
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.sentPackDocuments = sentPackDocuments;
        this.packSkipRules = packSkipRules;
        this.ukClock = ukClock;
    }

    public List<HearingNoticePackCandidate> findHearingNoticePackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        Map<UUID, PartyRole> roleByPartyId = new HashMap<>();
        if (!packSkipRules.shouldExcludeClaimant()) {
            partiesByRole(claim, PartyRole.CLAIMANT).forEach(party -> roleByPartyId.put(party.getId(),
                PartyRole.CLAIMANT));
        }
        partiesByRole(claim, PartyRole.DEFENDANT).forEach(party -> roleByPartyId.put(party.getId(),
            PartyRole.DEFENDANT));

        List<DocumentEntity> currentNotices = currentNotices(pcsCase);
        if (currentNotices.isEmpty() || packSkipRules.shouldSkipHearingNoticePack(pcsCase, claim)) {
            return List.of();
        }

        Set<String> sent =
            sentPackDocuments.sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));
        return currentNotices.stream()
            .filter(notice -> isPostable(notice.getParty(), roleByPartyId.get(notice.getParty().getId())))
            .filter(notice -> !sent.contains(SentPackDocuments.key(notice.getParty().getId(), notice.getId())))
            .map(notice -> new HearingNoticePackCandidate(roleByPartyId.get(notice.getParty().getId()),
                notice.getParty(), List.of(notice)))
            .toList();
    }

    // The latest notice each party was sent for each hearing that still needs one.
    private List<DocumentEntity> currentNotices(PcsCaseEntity pcsCase) {
        LocalDateTime now = LocalDateTime.now(ukClock);
        Map<String, DocumentEntity> latest = new HashMap<>();
        pcsCase.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.NOTICE_OF_HEARING)
            .filter(document -> document.getParty() != null && document.getHearing() != null)
            .filter(document -> isCurrentNotice(document, now))
            .forEach(document -> latest.merge(document.getHearing().getId() + ":" + document.getParty().getId(),
                document, HearingNoticePackSelector::later));
        return latest.values().stream()
            .sorted(Comparator.comparing((DocumentEntity notice) -> notice.getHearing().getHearingDate())
                .thenComparing(notice -> notice.getHearing().getId())
                .thenComparing(notice -> notice.getParty().getId()))
            .toList();
    }

    private static boolean isCurrentNotice(DocumentEntity notice, LocalDateTime now) {
        HearingEntity hearing = notice.getHearing();
        OptionalInt noticeNumber = noticeNumberOf(notice.getFileName());
        return hearing.getIssueNotice() == VerticalYesNo.YES
            && !Boolean.TRUE.equals(hearing.getCancelled())
            && hearing.getHearingDate() != null
            && !hearing.getHearingDate().isBefore(now)
            && noticeNumber.isPresent()
            && Objects.equals(hearing.getNoticeNumber(), noticeNumber.getAsInt());
    }

    private static DocumentEntity later(DocumentEntity first, DocumentEntity second) {
        Comparator<DocumentEntity> byCreation = Comparator.comparing(DocumentEntity::getSubmittedDate,
            Comparator.nullsFirst(Comparator.<Instant>naturalOrder()));
        return byCreation.compare(first, second) >= 0 ? first : second;
    }

    private static boolean isPostable(PartyEntity party, PartyRole role) {
        if (role == null || party.isRemoved() || !PostalPreferences.wantsPost(party)) {
            return false;
        }
        return role != PartyRole.DEFENDANT || !PostalPreferences.isRepresented(party);
    }
}
