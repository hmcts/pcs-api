package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingNoticePartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
                                 SentPackDocuments sentPackDocuments){
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.sentPackDocuments = sentPackDocuments;
    }

    public List<HearingNoticePackCandidate> findHearingNoticePackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        Map<UUID, PartyRole> roleByPartyId = roleByPartyId(claim);

        Set<String> sent =
            sentPackDocuments.sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));

        List<HearingNoticePackCandidate> candidates = new ArrayList<>();
        for (DocumentEntity notice : pcsCase.getDocuments()) {
            HearingEntity hearing = notice.getHearing();
            if (!isIssuedNotice(notice, hearing)) {
                continue;
            }
            for (PartyEntity party : recipientsOf(hearing, claim)) {
                PartyRole role = roleByPartyId.get(party.getId());
                if (role == null || isRepresented(party) || !wantsPost(party)
                    || sent.contains(key(party.getId(), notice.getId()))) {
                    continue;
                }
                candidates.add(new HearingNoticePackCandidate(role, party, List.of(notice)));
            }
        }
        return candidates;
    }

    // A generated notice of hearing for a live hearing that issues a notice.
    private boolean isIssuedNotice(DocumentEntity document, HearingEntity hearing) {
        return document.getType() == DocumentType.NOTICE_OF_HEARING
            && hearing != null
            && hearing.getIssueNotice() == VerticalYesNo.YES
            && !Boolean.TRUE.equals(hearing.getCancelled());
    }

    // Without notice: only the parties selected on the hearing. With notice: every claimant and defendant.
    private List<PartyEntity> recipientsOf(HearingEntity hearing, ClaimEntity claim) {
        if (hearing.getIsWithoutNotice() == VerticalYesNo.YES) {
            return hearing.getHearingNoticeParties().stream()
                .map(HearingNoticePartyEntity::getParty)
                .toList();
        }
        List<PartyEntity> allParties = new ArrayList<>(partiesByRole(claim, PartyRole.CLAIMANT));
        allParties.addAll(partiesByRole(claim, PartyRole.DEFENDANT));
        return allParties;
    }

    private Map<UUID, PartyRole> roleByPartyId(ClaimEntity claim) {
        Map<UUID, PartyRole> roles = new HashMap<>();
        partiesByRole(claim, PartyRole.DEFENDANT).forEach(party -> roles.put(party.getId(), PartyRole.DEFENDANT));
        partiesByRole(claim, PartyRole.CLAIMANT).forEach(party -> roles.put(party.getId(), PartyRole.CLAIMANT));
        return roles;
    }

    // Represented parties are served digitally through their legal representative, never by post.
    private boolean isRepresented(PartyEntity party) {
        return party.getClaimPartyOrganisationList().stream()
            .anyMatch(link -> YesOrNo.YES.equals(link.getActive()));
    }

    private boolean wantsPost(PartyEntity party) {
        ContactPreferencesEntity preferences = party.getContactPreferences();
        if (preferences == null || preferences.getContactByPost() == null) {
            return true;
        }
        return preferences.getContactByPost() != VerticalYesNo.NO;
    }
}
