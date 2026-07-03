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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects claim-pack envelopes per recipient. The claim form goes to the claimant and every defendant, each of
 * whom also gets their own access code once it exists; only documents without a {@code DOCUMENT_SENT} success
 * row are included, so a failed access-code letter self-heals without re-sending the claim form.
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

        Set<String> sent = sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));
        List<ClaimPackCandidate> candidates = new ArrayList<>();
        addClaimantCandidate(candidates, claim, claimForm, sent);
        addDefendantCandidates(candidates, pcsCase, claim, claimForm, sent);
        return candidates;
    }

    private void addClaimantCandidate(List<ClaimPackCandidate> candidates, ClaimEntity claim,
                                      DocumentEntity claimForm, Set<String> sent) {
        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        if (claimants.isEmpty()) {
            return;
        }
        PartyEntity claimant = claimants.getFirst();
        if (!sent.contains(key(claimant, claimForm))) {
            candidates.add(new ClaimPackCandidate(PartyRole.CLAIMANT, claimant, List.of(claimForm)));
        }
    }

    private void addDefendantCandidates(List<ClaimPackCandidate> candidates, PcsCaseEntity pcsCase, ClaimEntity claim,
                                        DocumentEntity claimForm, Set<String> sent) {
        for (PartyEntity defendant : partiesByRole(claim, PartyRole.DEFENDANT)) {
            DocumentEntity accessCode = accessCodeDocument(pcsCase, defendant);
            if (accessCode == null) {
                log.debug("Claim pack held for case {} defendant {} - awaiting access code",
                    pcsCase.getId(), defendant.getId());
                continue;
            }
            List<DocumentEntity> unsent = new ArrayList<>();
            if (!sent.contains(key(defendant, claimForm))) {
                unsent.add(claimForm);
            }
            if (!sent.contains(key(defendant, accessCode))) {
                unsent.add(accessCode);
            }
            if (!unsent.isEmpty()) {
                candidates.add(new ClaimPackCandidate(PartyRole.DEFENDANT, defendant, unsent));
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

    private Set<String> sentDocumentKeys(List<ClaimActivityLogEntity> activityLog) {
        return activityLog.stream()
            .filter(entry -> entry.getActivityType() == ClaimActivityType.DOCUMENT_SENT)
            .filter(entry -> entry.getStatus() == ClaimActivityStatus.SUCCESS)
            .filter(entry -> entry.getParty() != null && entry.getDocument() != null)
            .map(entry -> key(entry.getParty().getId(), entry.getDocument().getId()))
            .collect(Collectors.toSet());
    }

    private String key(PartyEntity party, DocumentEntity document) {
        return key(party.getId(), document.getId());
    }

    private String key(UUID partyId, UUID documentId) {
        return partyId + ":" + documentId;
    }
}
