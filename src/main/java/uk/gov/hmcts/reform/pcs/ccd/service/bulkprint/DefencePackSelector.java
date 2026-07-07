package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects defence-phase envelopes per recipient. Each party (claimant and defendants) gets the defence form
 * and any issued counter-claim; only documents not covered by a {@code PACK_SENT} success row are included,
 * so a late counter-claim follows in a later sweep without re-sending.
 */
@Service
public class DefencePackSelector {

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SentPackDocuments sentPackDocuments;

    public DefencePackSelector(ClaimActivityLogRepository claimActivityLogRepository,
                               SentPackDocuments sentPackDocuments) {
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.sentPackDocuments = sentPackDocuments;
    }

    public List<DefencePackCandidate> findDefencePackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        Set<String> sent =
            sentPackDocuments.sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));

        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        List<PartyEntity> allParties = new ArrayList<>(claimants);
        allParties.addAll(defendants);
        Set<UUID> claimantIds = claimants.stream().map(PartyEntity::getId).collect(Collectors.toSet());

        Map<UUID, PartyEntity> recipients = new LinkedHashMap<>();
        Map<UUID, Set<DocumentEntity>> documentsByRecipient = new LinkedHashMap<>();

        for (PartyEntity defendant : defendants) {
            DocumentEntity defenceForm = defenceFormDocument(pcsCase, defendant);
            if (defenceForm != null) {
                allParties.forEach(party -> addPending(recipients, documentsByRecipient, party, defenceForm));

                DocumentEntity counterClaimForm = counterClaimDocument(pcsCase, defendant);
                if (counterClaimForm != null) {
                    allParties.forEach(party -> addPending(recipients, documentsByRecipient, party, counterClaimForm));
                }
            }
        }

        List<DefencePackCandidate> candidates = new ArrayList<>();
        for (Map.Entry<UUID, PartyEntity> entry : recipients.entrySet()) {
            PartyEntity recipient = entry.getValue();
            List<DocumentEntity> unsent = documentsByRecipient.get(entry.getKey()).stream()
                .filter(document -> !sent.contains(key(recipient, document)))
                .toList();
            if (!unsent.isEmpty()) {
                PartyRole role = claimantIds.contains(recipient.getId()) ? PartyRole.CLAIMANT : PartyRole.DEFENDANT;
                candidates.add(new DefencePackCandidate(role, recipient, unsent));
            }
        }
        return candidates;
    }

    private void addPending(Map<UUID, PartyEntity> recipients, Map<UUID, Set<DocumentEntity>> documentsByRecipient,
                            PartyEntity recipient, DocumentEntity document) {
        recipients.putIfAbsent(recipient.getId(), recipient);
        documentsByRecipient.computeIfAbsent(recipient.getId(), key -> new LinkedHashSet<>()).add(document);
    }

    private DocumentEntity defenceFormDocument(PcsCaseEntity pcsCase, PartyEntity defendant) {
        return pcsCase.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.DEFENDANT_RESPONSE)
            .filter(document -> belongsToDefendant(document, defendant))
            .findFirst()
            .orElse(null);
    }

    private DocumentEntity counterClaimDocument(PcsCaseEntity pcsCase, PartyEntity defendant) {
        return pcsCase.getDocuments().stream()
            .filter(document -> document.getType() == DocumentType.COUNTERCLAIM)
            .filter(document -> document.getParty() != null
                && document.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);
    }

    private boolean belongsToDefendant(DocumentEntity document, PartyEntity defendant) {
        return document.getDefendantResponse() != null
            && document.getDefendantResponse().getParty() != null
            && document.getDefendantResponse().getParty().getId().equals(defendant.getId());
    }

    private String key(PartyEntity party, DocumentEntity document) {
        return SentPackDocuments.key(party.getId(), document.getId());
    }
}
