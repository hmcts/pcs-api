package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

/**
 * Records SUCCESS/FAILURE rows to {@code claim_activity_log} for access-code letter generation.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeActivityLogService {

    private final ClaimActivityLogRepository claimActivityLogRepository;

    public void logSuccess(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType) {
        save(pcsCase, party, activityType, ClaimActivityStatus.SUCCESS);
    }

    /**
     * Records SUCCESS in its own transaction (REQUIRES_NEW) so the row commits immediately after a bulk-print
     * letter is posted. This preserves the durable dedup key: a later rollback of the caller's per-case
     * transaction cannot erase the SUCCESS and cause an already-posted pack to be re-sent on the next sweep.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccessInNewTransaction(PcsCaseEntity pcsCase, PartyEntity party,
                                           ClaimActivityType activityType) {
        save(pcsCase, party, activityType, ClaimActivityStatus.SUCCESS);
    }

    /**
     * Uses REQUIRES_NEW so the failure row survives a rollback in the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType) {
        save(pcsCase, party, activityType, ClaimActivityStatus.FAILURE);
    }

    private void save(PcsCaseEntity pcsCase,
                      PartyEntity party,
                      ClaimActivityType activityType,
                      ClaimActivityStatus status) {

        claimActivityLogRepository.save(
            ClaimActivityLogEntity.builder()
                .pcsCase(pcsCase)
                .party(party)
                .activityType(activityType)
                .status(status)
                .build()
        );

        log.debug("Recorded claim activity {} with status {} for case {}{}",
                  activityType,
                  status,
                  pcsCase.getId(),
                  party != null ? " and party " + party.getId() : "");
    }
}
