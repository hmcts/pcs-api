package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects the claim-pack recipients for a case — the claimant, plus each defendant whose access code exists —
 * excluding anyone already sent. All-or-nothing on the claim form; per-defendant on the access code.
 */
@Service
@Slf4j
public class ClaimPackSelector {

    private final ClaimActivityLogRepository claimActivityLogRepository;

    public ClaimPackSelector(ClaimActivityLogRepository claimActivityLogRepository) {
        this.claimActivityLogRepository = claimActivityLogRepository;
    }

    public List<ClaimPackCandidate> findClaimPackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        DocumentEntity claimForm = claim.getClaimFormDocument();
        if (claimForm == null) {
            return List.of();
        }

        List<ClaimActivityLogEntity> activityLog = claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId());
        List<ClaimPackCandidate> candidates = new ArrayList<>();
        addClaimantCandidate(candidates, claim, claimForm, activityLog);
        addDefendantCandidates(candidates, pcsCase, claim, claimForm, activityLog);
        return candidates;
    }

    private void addClaimantCandidate(List<ClaimPackCandidate> candidates, ClaimEntity claim,
                                      DocumentEntity claimForm, List<ClaimActivityLogEntity> activityLog) {
        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        if (claimants.isEmpty()) {
            return;
        }
        PartyEntity claimant = claimants.getFirst();
        if (!alreadySent(activityLog, claimant, ClaimActivityType.CLAIMANT_PACK_SENT)) {
            candidates.add(new ClaimPackCandidate(PartyRole.CLAIMANT, claimant, List.of(claimForm)));
        }
    }

    private void addDefendantCandidates(List<ClaimPackCandidate> candidates, PcsCaseEntity pcsCase, ClaimEntity claim,
                                        DocumentEntity claimForm, List<ClaimActivityLogEntity> activityLog) {
        for (PartyEntity defendant : partiesByRole(claim, PartyRole.DEFENDANT)) {
            DocumentEntity accessCode = accessCodeDocument(pcsCase, defendant);
            if (accessCode == null) {
                log.debug("Claim pack held for case {} defendant {} - awaiting access code",
                    pcsCase.getId(), defendant.getId());
            } else if (!alreadySent(activityLog, defendant, ClaimActivityType.DEFENDANT_PACK_SENT)) {
                candidates.add(new ClaimPackCandidate(PartyRole.DEFENDANT, defendant, List.of(claimForm, accessCode)));
            }
        }
    }

    private DocumentEntity accessCodeDocument(PcsCaseEntity pcsCase, PartyEntity defendant) {
        return pcsCase.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.DEFENDANT_ACCESS_CODE)
            .filter(document -> belongsTo(document, defendant))
            .findFirst()
            .orElse(null);
    }

    private boolean belongsTo(DocumentEntity document, PartyEntity party) {
        return document.getParty() != null && document.getParty().getId().equals(party.getId());
    }

    private boolean alreadySent(List<ClaimActivityLogEntity> activityLog, PartyEntity party,
                                ClaimActivityType packSent) {
        return activityLog.stream().anyMatch(entry ->
            entry.getActivityType() == packSent
                && entry.getStatus() == ClaimActivityStatus.SUCCESS
                && entry.getParty() != null
                && entry.getParty().getId().equals(party.getId()));
    }
}
