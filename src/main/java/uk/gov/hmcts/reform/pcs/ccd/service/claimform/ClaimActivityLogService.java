package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.UUID;

/**
 * Records claim-form generation outcomes in the claim_activity_log table.
 *
 * <p>Success is written in the caller's transaction, so it commits atomically with the document
 * attach. Failure is written in its own transaction ({@code REQUIRES_NEW}) so the row survives the
 * rollback of the generation transaction. Both methods live on this separate bean so the proxy
 * applies the propagation; a method self-invoked within the generation service would bypass it.</p>
 */
@Service
@RequiredArgsConstructor
public class ClaimActivityLogService {

    private final PcsCaseService pcsCaseService;
    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final PartyRepository partyRepository;

    @Transactional
    public void logGenerationSuccess(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        record(pcsCase, claimantParty(pcsCase), ClaimActivityStatus.SUCCESS);
    }

    @Transactional
    public void logGenerationSuccess(long caseReference, PartyEntity party) {
        record(pcsCaseService.loadCase(caseReference), party, ClaimActivityStatus.SUCCESS);
    }

    @Transactional
    public void logGenerationSuccess(PcsCaseEntity pcsCase, PartyEntity party) {
        record(pcsCase, party, ClaimActivityStatus.SUCCESS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGenerationFailure(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        record(pcsCase, claimantParty(pcsCase), ClaimActivityStatus.FAILURE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGenerationFailure(long caseReference, UUID partyId) {
        PartyEntity party = partyId == null ? null : partyRepository.getReferenceById(partyId);
        record(pcsCaseService.loadCase(caseReference), party, ClaimActivityStatus.FAILURE);
    }

    private void record(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityStatus status) {
        claimActivityLogRepository.save(
            ClaimActivityLogEntity.builder()
                .pcsCase(pcsCase)
                .party(party)
                .activityType(ClaimActivityType.DOCUMENTS_CREATED)
                .status(status)
                .build());
    }

    private static PartyEntity claimantParty(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return null;
        }
        return pcsCase.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElse(null);
    }
}
