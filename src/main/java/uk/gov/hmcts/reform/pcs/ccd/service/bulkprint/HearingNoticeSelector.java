package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.SentPackDocuments.key;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects hearing-notice envelopes per recipient. With-notice hearings go to every claimant and defendant;
 * without-notice hearings go only to the parties selected on the hearing. Represented parties are skipped
 * (their legal representative is notified digitally), as are parties who opted out of post
 */
@Service
public class HearingNoticeSelector {

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SentPackDocuments sentPackDocuments;

    public HearingNoticeSelector(ClaimActivityLogRepository claimActivityLogRepository,
                                 SentPackDocuments sentPackDocuments) {
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.sentPackDocuments = sentPackDocuments;
    }

    public List<HearingNoticePackCandidate> findHearingNoticePackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        Set<UUID> defendantIds = partiesByRole(claim, PartyRole.DEFENDANT).stream()
            .map(PartyEntity::getId)
            .collect(Collectors.toSet());

        Set<String> sent =
            sentPackDocuments.sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));

        return pcsCase.getDocuments().stream()
            .filter(notice -> isPostableNotice(notice, defendantIds, sent))
            .map(notice -> new HearingNoticePackCandidate(PartyRole.DEFENDANT, notice.getParty(), List.of(notice)))
            .toList();
    }

    // An issued notice for a postable defendant that has not been posted yet.
    private boolean isPostableNotice(DocumentEntity notice, Set<UUID> defendantIds, Set<String> sent) {
        PartyEntity party = notice.getParty();
        return isIssuedNotice(notice, notice.getHearing())
            && isPostableDefendant(party, defendantIds)
            && !sent.contains(key(party.getId(), notice.getId()));
    }

    // A generated notice of hearing for a live hearing that issues a notice.
    private boolean isIssuedNotice(DocumentEntity document, HearingEntity hearing) {
        return document.getType() == DocumentType.HEARING_NOTICE
            && hearing != null
            && hearing.getIssueNotice() == VerticalYesNo.YES
            && !Boolean.TRUE.equals(hearing.getCancelled());
    }

    // Only unrepresented defendants who opted in to post are posted.
    private boolean isPostableDefendant(PartyEntity party, Set<UUID> defendantIds) {
        return party != null
            && defendantIds.contains(party.getId())
            && !isRepresented(party)
            && wantsPost(party);
    }

    // Represented parties are served digitally through their legal representative, never by post.
    private boolean isRepresented(PartyEntity party) {
        return party.getClaimPartyOrganisationList().stream()
            .anyMatch(link -> YesOrNo.YES.equals(link.getActive()));
    }

    private boolean wantsPost(PartyEntity party) {
        ContactPreferencesEntity preferences = party.getContactPreferences();
        return preferences != null && preferences.getContactByPost() == VerticalYesNo.YES;
    }
}
