package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects defence-pack envelopes per defendant: best-effort, driven by the counter-claim's state. The defence
 * form goes out mandatorily; a counter-claim is optional and follows once issued. Pack status progresses
 * partially-sent (counter-claim pending) to sent (complete).
 */
@Service
public class DefencePackCandidateService {

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final CounterClaimRepository counterClaimRepository;

    public DefencePackCandidateService(ClaimActivityLogRepository claimActivityLogRepository,
                                       CounterClaimRepository counterClaimRepository) {
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.counterClaimRepository = counterClaimRepository;
    }

    public List<DefencePackCandidate> findDefencePackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        List<ClaimActivityLogEntity> activityLog = claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId());

        List<DefencePackCandidate> candidates = new ArrayList<>();
        for (PartyEntity defendant : partiesByRole(claim, PartyRole.DEFENDANT)) {
            defencePackFor(pcsCase, defendant, activityLog).ifPresent(candidates::add);
        }
        return candidates;
    }

    private Optional<DefencePackCandidate> defencePackFor(PcsCaseEntity pcsCase, PartyEntity defendant,
                                                          List<ClaimActivityLogEntity> activityLog) {
        if (hasStatus(activityLog, defendant, ClaimActivityType.DEFENCE_PACK_SENT)) {
            return Optional.empty();
        }
        Optional<CounterClaimEntity> counterClaim = counterClaim(pcsCase, defendant);
        DocumentEntity counterClaimDoc = counterClaimDocument(pcsCase, defendant);
        boolean counterClaimSendable = counterClaimDoc != null
            && counterClaim.map(cc -> cc.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED).orElse(false);

        if (hasStatus(activityLog, defendant, ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT)) {
            return counterClaimSendable
                ? Optional.of(new DefencePackCandidate(defendant, List.of(counterClaimDoc),
                    ClaimActivityType.DEFENCE_PACK_SENT))
                : Optional.empty();
        }

        DocumentEntity defenceForm = defenceFormDocument(pcsCase, defendant);
        if (defenceForm == null) {
            return Optional.empty();
        }
        if (counterClaimSendable) {
            return Optional.of(new DefencePackCandidate(defendant, List.of(defenceForm, counterClaimDoc),
                ClaimActivityType.DEFENCE_PACK_SENT));
        }
        ClaimActivityType targetStatus = counterClaim.isPresent()
            ? ClaimActivityType.DEFENCE_PACK_PARTIALLY_SENT
            : ClaimActivityType.DEFENCE_PACK_SENT;
        return Optional.of(new DefencePackCandidate(defendant, List.of(defenceForm), targetStatus));
    }

    private Optional<CounterClaimEntity> counterClaim(PcsCaseEntity pcsCase, PartyEntity defendant) {
        return counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdOrderByClaimSubmittedDateDesc(
            pcsCase.getCaseReference(), defendant.getId());
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

    private boolean hasStatus(List<ClaimActivityLogEntity> activityLog, PartyEntity defendant,
                              ClaimActivityType activityType) {
        return activityLog.stream().anyMatch(entry ->
            entry.getActivityType() == activityType
                && entry.getStatus() == ClaimActivityStatus.SUCCESS
                && entry.getParty() != null
                && entry.getParty().getId().equals(defendant.getId()));
    }
}
