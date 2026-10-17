package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;

/**
 * Records the claimant's answer to "Confirm the eviction details" against the claim's live enforcement.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimantEvictionConfirmationService {

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;

    /**
     * Moves the live enforcement to "Claimant confirmed" or "Claimant unable to attend". This only happens
     * when it is awaiting the claimant's confirmation. The journey is still offered from the bailiff date
     * rather than the status, because nothing schedules an eviction yet (HDPI-7344), so in any other case
     * the status is left as it is.
     */
    @Transactional
    public void recordAttendance(long caseReference, YesOrNo canAttend) {
        if (canAttend == null) {
            log.info("Case {}: no attendance answer, enforcement status not changed", caseReference);
            return;
        }
        Optional<EnforcementOrderEntity> liveEnforcement = findLiveEnforcement(caseReference);
        if (liveEnforcement.isEmpty()) {
            log.info("Case {}: no live enforcement, enforcement status not changed", caseReference);
            return;
        }
        EnforcementOrderEntity enforcement = liveEnforcement.get();
        if (enforcement.getStatus() != AWAITING_CLAIMANT_CONFIRMATION) {
            log.info("Case {}: enforcement {} is {}, not {}, so its status is not changed",
                     caseReference, enforcement.getId(), enforcement.getStatus(), AWAITING_CLAIMANT_CONFIRMATION);
            return;
        }
        EnforcementStatus target = canAttend.toBoolean() ? CLAIMANT_CONFIRMED : CLAIMANT_UNABLE_TO_ATTEND;
        enforcementStatusTransitions.transition(enforcement, target);
        enforcementOrderRepository.save(enforcement);
        // TODO HDPI-7549: when the claimant cannot attend, create a "Reschedule eviction" task for the allocated
        // bailiff, and store the dates they are not available (the journey does not ask for them yet).
        // TODO HDPI-7552: create a "Review updated risk information" task when the claimant changes their risk
        // answers (the journey does not ask for them yet). Both tasks need Work Allocation.
    }

    private Optional<EnforcementOrderEntity> findLiveEnforcement(long caseReference) {
        List<ClaimEntity> claims = pcsCaseService.loadCase(caseReference).getClaims();
        if (claims == null || claims.isEmpty()) {
            return Optional.empty();
        }
        // A claim can have at most one live enforcement
        return enforcementOrderRepository.findByClaimId(claims.getFirst().getId()).stream()
            .filter(enforcement -> enforcement.getStatus() != null && enforcement.getStatus().isLive())
            .findFirst();
    }
}
