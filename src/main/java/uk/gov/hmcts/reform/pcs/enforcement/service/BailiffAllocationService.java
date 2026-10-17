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

/**
 * Allocates the claim's current warrant to a bailiff (HDPI-7356, HDPI-8189). The first allocation moves the
 * warrant from awaiting bailiff allocation to awaiting scheduling. A later one reallocates it: the previous
 * allocation is ended, so only the new bailiff is active.
 *
 * <p>The allocation is held in enf_bailiff_allocation rather than as an AM role assignment, because pcs-api
 * cannot create the {@code allocated-bailiff} CASE role yet. Its RoleAssignmentApi only reads, and CCD's
 * case-users API, which pcs-api uses for [DEFENDANT] and the other bracketed case roles, rejects a role
 * name without brackets.
 */
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
    private final Clock utcClock;

    /**
     * The claim's current enforcement, if it is a warrant bailiffs can be allocated to.
     *
     * @throws NoEnforcementException if the claim has no enforcement
     * @throws EnforcementEventNotAvailableException if the enforcement is not an approved, live warrant
     */
    public EnforcementOrderEntity getAllocatableEnforcement(long caseReference) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .orElseThrow(NoEnforcementException::new);
        if (!BailiffEnforcementRules.isApprovedLiveWarrant(enforcement)) {
            throw new EnforcementEventNotAvailableException(NOT_AVAILABLE_MESSAGE);
        }
        return enforcement;
    }

    /**
     * The bailiff the enforcement is allocated to now, if any.
     */
    public Optional<BailiffAllocationEntity> findActiveAllocation(EnforcementOrderEntity enforcement) {
        return bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId()).stream()
            .findFirst();
    }

    /**
     * Allocates the claim's current warrant to the bailiff. Allocating it again to its current bailiff
     * changes nothing.
     *
     * @throws NoEnforcementException if the claim has no enforcement
     * @throws EnforcementEventNotAvailableException if the enforcement is not an approved, live warrant
     */
    @Transactional
    public void allocate(long caseReference, BailiffUser bailiff) {
        EnforcementOrderEntity enforcement = getAllocatableEnforcement(caseReference);
        Instant now = Instant.now(utcClock);

        List<BailiffAllocationEntity> active =
            bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId());
        boolean alreadyAllocated = active.stream()
            .anyMatch(allocation -> allocation.getBailiffUserId().equals(bailiff.userId()));

        if (!alreadyAllocated) {
            active.forEach(previous -> previous.setEndedAt(now));
            bailiffAllocationRepository.saveAllAndFlush(active);
            bailiffAllocationRepository.save(BailiffAllocationEntity.builder()
                                                 .enforcementOrder(enforcement)
                                                 .bailiffUserId(bailiff.userId())
                                                 .bailiffName(bailiff.name())
                                                 .bailiffEmail(bailiff.email())
                                                 .allocatedByUserId(securityContextService.getCurrentUserId())
                                                 .startedAt(now)
                                                 .build());
            // TODO(HDPI-8189): create the allocated-bailiff CASE role assignment for the bailiff in AM, and
            //  delete the previous bailiff's, so the "Roles and access" tab and case access follow the
            //  allocation. This needs a RAS write client (POST /am/role-assignments with
            //  replaceExisting=true and a per-case process and reference), an AM approval rule for
            //  allocated-bailiff, and RAS in the environments PCS deploys to. Until then the allocation
            //  lives only in enf_bailiff_allocation.
            // TODO(HDPI-8627): reassign the case's open bailiff tasks to the new bailiff.
        }

        if (enforcement.getStatus() == AWAITING_BAILIFF_ALLOCATION) {
            enforcementStatusTransitions.transition(enforcement, AWAITING_SCHEDULING);
            enforcementOrderRepository.save(enforcement);
        }
        // TODO(HDPI-8627): complete the "Allocate case to bailiff" task and create the "Review risks and set
        //  level" task, assigned to the allocated bailiff.
        log.info("Enforcement {} allocated to bailiff {}", enforcement.getId(), bailiff.userId());
    }
}
