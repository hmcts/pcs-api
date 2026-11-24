package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimDocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects claim-pack envelopes per recipient. The claim form goes to every defendant and, while release flag 1.4 is
 * off, the claimant too. When release flag 1.4 is on, only defendants will receive the claim pack. A citizen
 * claimant is never posted a claim pack; they follow the claim on their dashboard.
 * Defendants also get their own access code once it exists (on a Wales case, both the English and the Welsh
 * letter). On a citizen claim the defendant's pack leads with the claim form, then the documents the claimant
 * uploaded with the claim, then the access-code letter, and is posted with the HLPAS legal advice leaflet.
 * Only documents not covered by a {@code PACK_SENT}
 * success row are included, so a failed access-code letter self-heals without re-sending the claim form.
 */
@Service
@Slf4j
public class ClaimPackSelector {

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_PARTY_ID = "partyId";
    private static final List<DocumentType> UPLOAD_ORDER = List.of(
        DocumentType.TENANCY_AGREEMENT, DocumentType.OCCUPATION_LICENCE, DocumentType.ENERGY_PERFORMANCE_CERTIFICATE,
        DocumentType.GAS_SAFETY_CERTIFICATE, DocumentType.EICR_REPORT, DocumentType.POSSESSION_NOTICE,
        DocumentType.RENT_STATEMENT);

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
        boolean citizenClaim = isCitizenClaim(pcsCase);
        if (!citizenClaim && !packSkipRules.shouldExcludeClaimant()) {
            addClaimantCandidate(candidates, claim, claimForm, sent);
        }
        List<DocumentEntity> uploads = citizenClaim ? claimantUploads(claim) : List.of();
        addDefendantCandidates(candidates, pcsCase, claim, new DefendantPack(claimForm, uploads, citizenClaim), sent);
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
                                        DefendantPack pack, Set<String> sent) {
        boolean wales = pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;
        for (PartyEntity defendant : partiesByRole(claim, PartyRole.DEFENDANT)) {
            ClaimPackCandidate candidate = defendantCandidate(pcsCase, defendant, pack, sent, wales);
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
                                                  DefendantPack pack, Set<String> sent, boolean wales) {
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
        List<DocumentEntity> ordered = new ArrayList<>();
        if (pack.citizenClaim()) {
            ordered.add(pack.claimForm());
            ordered.addAll(pack.uploads());
        }
        ordered.add(accessCode);
        if (welshAccessCode != null) {
            ordered.add(welshAccessCode);
        }
        if (!pack.citizenClaim()) {
            ordered.add(pack.claimForm());
        }
        List<DocumentEntity> unsent = ordered.stream()
            .filter(document -> !sent.contains(key(defendant, document)))
            .toList();
        if (unsent.isEmpty()) {
            return null;
        }
        if (wales && welshAccessCode == null) {
            withCaseContext(pcsCase, defendant, () -> log.info(
                "No Welsh access-code letter, sending the English pack - case: {}, party: {}",
                pcsCase.getCaseReference(), defendant.getId()));
        }
        return new ClaimPackCandidate(PartyRole.DEFENDANT, defendant, unsent, welshAccessCode != null,
            pack.citizenClaim());
    }

    // A citizen claim is created by the claimant's own IDAM account; a legal-rep claim by their organisation.
    private static boolean isCitizenClaim(PcsCaseEntity pcsCase) {
        return pcsCase.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .anyMatch(creator -> StringUtils.isNotBlank(creator.getIdamId())
                && StringUtils.isBlank(creator.getOrganisationId()));
    }

    // The claim's own documents are those submitted with it; documents uploaded after issue are case-level.
    private static List<DocumentEntity> claimantUploads(ClaimEntity claim) {
        return claim.getClaimDocuments().stream()
            .map(ClaimDocumentEntity::getDocument)
            .filter(Objects::nonNull)
            .filter(document -> document.getDocumentId() != null)
            .sorted(Comparator.comparingInt(ClaimPackSelector::uploadRank))
            .toList();
    }

    private static int uploadRank(DocumentEntity document) {
        int rank = UPLOAD_ORDER.indexOf(document.getType());
        return rank < 0 ? UPLOAD_ORDER.size() : rank;
    }

    private record DefendantPack(DocumentEntity claimForm, List<DocumentEntity> uploads, boolean citizenClaim) {
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
