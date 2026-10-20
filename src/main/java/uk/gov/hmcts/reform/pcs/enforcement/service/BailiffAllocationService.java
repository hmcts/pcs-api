package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;

@Slf4j
@Service
@RequiredArgsConstructor
public class BailiffAllocationService {

    static final String NOT_AVAILABLE_MESSAGE = "You can only allocate a warrant that has been approved and is "
        + "still live";

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final BailiffAllocationRepository bailiffAllocationRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final SecurityContextService securityContextService;
    private final EnforcementWaTaskService enforcementWaTaskService;
    private final Clock utcClock;

    public static boolean canAllocateBailiff(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement != null && BailiffEnforcementRules.isApprovedLiveWarrant(currentEnforcement);
    }

    public EnforcementOrderEntity getAllocatableEnforcement(long caseReference) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .orElseThrow(NoEnforcementException::new);
        if (!canAllocateBailiff(enforcement)) {
            throw new EnforcementEventNotAvailableException(NOT_AVAILABLE_MESSAGE);
        }
        return enforcement;
    }

    private EnforcementChange<EnforcementOrderEntity> findAllocatableEnforcement(long caseReference) {
        Optional<EnforcementOrderEntity> current = currentEnforcementService.findCurrentEnforcement(caseReference);
        if (current.isEmpty()) {
            return EnforcementChange.refused(NoEnforcementException.MESSAGE);
        }
        if (!canAllocateBailiff(current.get())) {
            return EnforcementChange.refused(NOT_AVAILABLE_MESSAGE);
        }
        return EnforcementChange.made(current.get());
    }

    public Optional<BailiffAllocationEntity> findActiveAllocation(EnforcementOrderEntity enforcement) {
        return bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId()).stream()
            .findFirst();
    }

    /**
     * The partial unique index on enf_bailiff_allocation allows one active row per enforcement, so a stale
     * allocation has to be ended before the next one can be made.
     */
    @Transactional
    public void endActiveAllocations(EnforcementOrderEntity enforcement) {
        List<BailiffAllocationEntity> active =
            bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId());
        if (!active.isEmpty()) {
            Instant now = Instant.now(utcClock);
            active.forEach(allocation -> allocation.setEndedAt(now));
            bailiffAllocationRepository.saveAllAndFlush(active);
        }
    }

    @Transactional
    public EnforcementChange<String> allocate(long caseReference, BailiffUser bailiff) {
        EnforcementChange<EnforcementOrderEntity> allocatable = findAllocatableEnforcement(caseReference);
        if (allocatable.refused()) {
            return EnforcementChange.refused(allocatable.errors());
        }
        EnforcementOrderEntity enforcement = allocatable.change();
        Instant now = Instant.now(utcClock);

        List<BailiffAllocationEntity> active =
            bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId());
        boolean alreadyAllocated = active.stream()
            .anyMatch(allocation -> allocation.getBailiffUserId().equals(bailiff.userId()));

        if (!alreadyAllocated) {
            boolean replacingABailiff = !active.isEmpty();
            endActiveAllocations(enforcement);
            BailiffAllocationEntity allocation =
                bailiffAllocationRepository.save(BailiffAllocationEntity.builder()
                                                     .enforcementOrder(enforcement)
                                                     .bailiffUserId(bailiff.userId())
                                                     .bailiffName(bailiff.name())
                                                     .bailiffEmail(bailiff.email())
                                                     .allocatedByUserId(securityContextService.getCurrentUserId())
                                                     .startedAt(now)
                                                     .build());
            // TODO(HDPI-8189): write the allocated-bailiff CASE role assignment in AM, which is what moves
            //  the warrant's open bailiff tasks on a reallocation.
            if (needsRiskAssessmentTask(enforcement, replacingABailiff)) {
                EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
                    caseReference, "the allocated bailiff's risk assessment task",
                    () -> enforcementWaTaskService.bailiffAllocated(caseReference, allocation.getId()));
                if (scheduled.refused()) {
                    return EnforcementChange.refused(scheduled.errors());
                }
            }
        }

        if (enforcement.getStatus() == AWAITING_BAILIFF_ALLOCATION) {
            enforcementStatusTransitions.transition(enforcement, AWAITING_SCHEDULING);
            enforcementOrderRepository.save(enforcement);
        }
        log.info("Enforcement {} allocated to bailiff {}", enforcement.getId(), bailiff.userId());
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

    private static boolean needsRiskAssessmentTask(EnforcementOrderEntity enforcement, boolean replacingABailiff) {
        return enforcement.getRiskLevelSetAt() == null && !replacingABailiff;
    }
}
