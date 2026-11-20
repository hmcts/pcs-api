package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects claim-pack envelopes per recipient. The claim form goes to every defendant and, while release flag 1.4 is
 * off, the claimant too. When release flag 1.4 is on, only defendants will receive the claim pack.
 * Defendants also get their own access code once it exists (on a Wales case, both the English and the Welsh
 * letter). Only documents not covered by a {@code PACK_SENT}
 * success row are included, so a failed access-code letter self-heals without re-sending the claim form.
 */
@Service
@Slf4j
public class ClaimPackSelector {

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_PARTY_ID = "partyId";

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SentPackDocuments sentPackDocuments;
    private final PackSkipRules packSkipRules;

    public ClaimPackSelector(ClaimActivityLogRepository claimActivityLogRepository,
                             SentPackDocuments sentPackDocuments,
                             PackSkipRules packSkipRules) {
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.sentPackDocuments = sentPackDocuments;
        this.packSkipRules = packSkipRules;
    }

    public List<ClaimPackCandidate> findClaimPackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        if (packSkipRules.shouldSkipClaimPack(pcsCase, claim)) {
            return List.of();
        }
        DocumentEntity claimForm = claim.getClaimFormDocument();
        if (claimForm == null) {
            return List.of();
        }

        Set<String> sent =
            sentPackDocuments.sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));
        List<ClaimPackCandidate> candidates = new ArrayList<>();
        if (!packSkipRules.shouldExcludeClaimant()) {
            addClaimantCandidate(candidates, claim, claimForm, sent);
        }
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
        boolean wales = pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;
        for (PartyEntity defendant : partiesByRole(claim, PartyRole.DEFENDANT)) {
            ClaimPackCandidate candidate = defendantCandidate(pcsCase, defendant, claimForm, sent, wales);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
    }

    // Same MDC keys as PackSendRecorder, so App Insights finds these lines by case and party.
    private static void withCaseContext(PcsCaseEntity pcsCase, PartyEntity party, Runnable logLine) {
        MDC.put(MDC_CASE_REFERENCE, String.valueOf(pcsCase.getCaseReference()));
        MDC.put(MDC_PARTY_ID, String.valueOf(party.getId()));
        try {
            logLine.run();
        } finally {
            MDC.remove(MDC_CASE_REFERENCE);
            MDC.remove(MDC_PARTY_ID);
        }
    }

    private ClaimPackCandidate defendantCandidate(PcsCaseEntity pcsCase, PartyEntity defendant,
                                                  DocumentEntity claimForm, Set<String> sent, boolean wales) {
        DocumentEntity accessCode = accessCodeDocument(pcsCase, defendant, false);
        if (accessCode == null) {
            withCaseContext(pcsCase, defendant, () -> log.debug(
                "Claim pack held, awaiting access code - case: {}, party: {}",
                pcsCase.getCaseReference(), defendant.getId()));
            return null;
        }
        // Both letters are saved together, so a Wales defendant with no Welsh letter got their access code
        // before Welsh packs existed. It can never be made now, so they get the English pack.
        DocumentEntity welshAccessCode = wales ? accessCodeDocument(pcsCase, defendant, true) : null;
        List<DocumentEntity> unsent = new ArrayList<>();
        if (!sent.contains(key(defendant, accessCode))) {
            unsent.add(accessCode);
        }
        if (welshAccessCode != null && !sent.contains(key(defendant, welshAccessCode))) {
            unsent.add(welshAccessCode);
        }
        if (!sent.contains(key(defendant, claimForm))) {
            unsent.add(claimForm);
        }
        if (unsent.isEmpty()) {
            return null;
        }
        if (wales && welshAccessCode == null) {
            withCaseContext(pcsCase, defendant, () -> log.info(
                "No Welsh access-code letter, sending the English pack - case: {}, party: {}",
                pcsCase.getCaseReference(), defendant.getId()));
        }
        return new ClaimPackCandidate(PartyRole.DEFENDANT, defendant, unsent, welshAccessCode != null);
    }

    private DocumentEntity accessCodeDocument(PcsCaseEntity pcsCase, PartyEntity defendant, boolean welsh) {
        return pcsCase.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.DEFENDANT_ACCESS_CODE)
            .filter(document -> belongsTo(document, defendant))
            .filter(document -> (document.getLanguage() == AccessCodeLetterLanguage.WELSH) == welsh)
            .findFirst()
            .orElse(null);
    }

    private boolean belongsTo(DocumentEntity document, PartyEntity party) {
        return document.getParty() != null && document.getParty().getId().equals(party.getId());
    }

    private String key(PartyEntity party, DocumentEntity document) {
        return SentPackDocuments.key(party.getId(), document.getId());
    }
}
