package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeDocuments;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects claim-pack envelopes per recipient. The claim form goes to every defendant and, while release flag 1.4 is
 * off, the claimant too. When release flag 1.4 is on, only defendants will receive the claim pack.
 * Defendants also get their own access-code letter once it exists; on a Wales case, the English and the Welsh
 * letter, and the pack waits for both. Only documents not covered by a {@code PACK_SENT}
 * success row are included, so a failed access-code letter self-heals without re-sending the claim form.
 */
@Service
@Slf4j
public class ClaimPackSelector {

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
            Map<AccessCodeLetterLanguage, DocumentEntity> letters =
                AccessCodeDocuments.byLanguage(pcsCase.getDocuments(), defendant);
            DocumentEntity englishLetter = letters.get(AccessCodeLetterLanguage.ENGLISH);
            // England ignores any Welsh-marked letter. Wales waits for both letters (HDPI-9698 D4).
            DocumentEntity welshLetter = wales ? letters.get(AccessCodeLetterLanguage.WELSH) : null;
            String missing = missingLetters(englishLetter, welshLetter, wales);
            if (missing != null) {
                log.debug("Claim pack held for case {} defendant {} - awaiting {}",
                    pcsCase.getId(), defendant.getId(), missing);
                continue;
            }
            List<DocumentEntity> unsent = new ArrayList<>();
            addIfUnsent(unsent, sent, defendant, englishLetter);
            if (welshLetter != null) {
                addIfUnsent(unsent, sent, defendant, welshLetter);
            }
            addIfUnsent(unsent, sent, defendant, claimForm);
            if (!unsent.isEmpty()) {
                candidates.add(new ClaimPackCandidate(PartyRole.DEFENDANT, defendant, unsent));
            }
        }
    }

    // Names what a held defendant pack is waiting for (HDPI-9698 R3). England keeps its old wording.
    private static String missingLetters(DocumentEntity englishLetter, DocumentEntity welshLetter, boolean wales) {
        if (!wales) {
            return englishLetter == null ? "access code" : null;
        }
        if (englishLetter == null && welshLetter == null) {
            return "access code (English and Welsh letters)";
        }
        if (englishLetter == null) {
            return "English access-code letter";
        }
        return welshLetter == null ? "Welsh access-code letter" : null;
    }

    private void addIfUnsent(List<DocumentEntity> unsent, Set<String> sent, PartyEntity defendant,
                             DocumentEntity document) {
        if (!sent.contains(key(defendant, document))) {
            unsent.add(document);
        }
    }

    private String key(PartyEntity party, DocumentEntity document) {
        return SentPackDocuments.key(party.getId(), document.getId());
    }
}
