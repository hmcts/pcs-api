package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PROPERTY_VISIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

/**
 * Records the returns a bailiff makes on a warrant (HDPI-7347, HDPI-8424) and keeps them as the warrant's
 * history, moving the warrant's status as each return requires.
 */
@Slf4j
@Service
public class WarrantReturnService {

    public static final String NOT_ALLOCATED_MESSAGE =
        "A bailiff must be allocated to the warrant before a return can be recorded";

    /**
     * Only warrants go to county court bailiffs. A writ goes to a High Court Enforcement Officer.
     */
    static final Set<SelectEnforcementType> WARRANT_TYPES = Collections.unmodifiableSet(
        EnumSet.of(SelectEnforcementType.WARRANT, SelectEnforcementType.WARRANT_OF_RESTITUTION));

    /**
     * Every status once a bailiff has been allocated, while the warrant is live, plus CLOSED for "Reopen
     * warrant". A return can be recorded at several points, not just after the claimant confirms.
     */
    static final Set<EnforcementStatus> RECORDABLE_STATUSES = Collections.unmodifiableSet(EnumSet.of(
        AWAITING_SCHEDULING, AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED, CLAIMANT_UNABLE_TO_ATTEND,
        AWAITING_PROPERTY_VISIT, BREATHING_SPACE, CLOSED));

    /**
     * HDPI-8424: a further warrant is free within 12 calendar months of the first warrant being generated.
     */
    static final int FREE_REPEAT_WARRANT_MONTHS = 12;

    /**
     * HDPI-8424 return names after which the next warrant is free. Other reasons are TBC (OPEN-QUESTIONS 6).
     */
    static final Set<WarrantReturnName> FREE_REPEAT_WARRANT_RETURN_NAMES = Collections.unmodifiableSet(
        EnumSet.of(WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER));

    private final CurrentEnforcementService currentEnforcementService;
    private final WarrantReturnRepository warrantReturnRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final WarrantReturnValidator warrantReturnValidator;
    private final SecurityContextService securityContextService;
    private final BailiffAllocationService bailiffAllocationService;
    private final Clock utcClock;
    private final Clock ukClock;

    public WarrantReturnService(CurrentEnforcementService currentEnforcementService,
                                WarrantReturnRepository warrantReturnRepository,
                                EnforcementStatusTransitions enforcementStatusTransitions,
                                WarrantReturnValidator warrantReturnValidator,
                                SecurityContextService securityContextService,
                                BailiffAllocationService bailiffAllocationService,
                                @Qualifier("utcClock") Clock utcClock,
                                @Qualifier("ukClock") Clock ukClock) {
        this.currentEnforcementService = currentEnforcementService;
        this.warrantReturnRepository = warrantReturnRepository;
        this.enforcementStatusTransitions = enforcementStatusTransitions;
        this.warrantReturnValidator = warrantReturnValidator;
        this.securityContextService = securityContextService;
        this.bailiffAllocationService = bailiffAllocationService;
        this.utcClock = utcClock;
        this.ukClock = ukClock;
    }

    /**
     * The warrant a return would be recorded against: the claim's current enforcement, if it is a warrant
     * that a bailiff has been allocated to. For the start callback and the mid-event page: it refuses by
     * throwing, which
     * {@link uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementCallbackExceptionHandler} turns into an
     * error on the callback response.
     *
     * @throws NoEnforcementException if the claim has no warrant, or it has not been approved yet
     * @throws EnforcementEventNotAvailableException if the warrant is waiting for a bailiff to be allocated
     */
    @Transactional(readOnly = true)
    public EnforcementOrderEntity findWarrant(long caseReference) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(WarrantReturnService::isWarrant)
            .orElseThrow(NoEnforcementException::new);
        EnforcementStatus status = enforcement.getStatus();
        if (status == AWAITING_BAILIFF_ALLOCATION) {
            throw new EnforcementEventNotAvailableException(NOT_ALLOCATED_MESSAGE);
        }
        if (!RECORDABLE_STATUSES.contains(status)) {
            // Not approved yet (or rejected), so no warrant has been generated.
            throw new NoEnforcementException();
        }
        return enforcement;
    }

    /**
     * The warrant a return would be recorded against, or why there is none.
     */
    private EnforcementChange<EnforcementOrderEntity> findRecordableWarrant(long caseReference) {
        Optional<EnforcementOrderEntity> warrant = currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(WarrantReturnService::isWarrant);
        if (warrant.isEmpty()) {
            return EnforcementChange.refused(NoEnforcementException.MESSAGE);
        }
        EnforcementStatus status = warrant.get().getStatus();
        if (status == AWAITING_BAILIFF_ALLOCATION) {
            return EnforcementChange.refused(NOT_ALLOCATED_MESSAGE);
        }
        if (!RECORDABLE_STATUSES.contains(status)) {
            // Not approved yet (or rejected), so no warrant has been generated.
            return EnforcementChange.refused(NoEnforcementException.MESSAGE);
        }
        return EnforcementChange.made(warrant.get());
    }

    /**
     * The errors to show for the answers, against the warrant's current status.
     */
    public List<String> validate(long caseReference, WarrantReturnDetails details) {
        return warrantReturnValidator.validate(details, findWarrant(caseReference).getStatus());
    }

    /**
     * Saves the return to the warrant's history and moves the warrant to the status the return name requires
     * (HDPI-8424). A return name that leaves the status unchanged, or that names the status the warrant is
     * already in, records the return only.
     *
     * @return what was recorded, or the errors to show when there is no warrant to record it against or the
     *         answers are not valid for the warrant as it is now
     */
    @Transactional
    public EnforcementChange<RecordedWarrantReturn> recordReturn(long caseReference, WarrantReturnDetails details) {
        EnforcementChange<EnforcementOrderEntity> recordable = findRecordableWarrant(caseReference);
        if (recordable.refused()) {
            return EnforcementChange.refused(recordable.errors());
        }
        EnforcementOrderEntity warrant = recordable.change();
        List<String> errors = warrantReturnValidator.validate(details, warrant.getStatus());
        if (!errors.isEmpty()) {
            return EnforcementChange.refused(errors);
        }
        WarrantReturnName returnName = WarrantReturnDetails.selectedReturnName(details).orElseThrow();
        boolean finalReturn = details.getType() == WarrantReturnType.FINAL;
        boolean successfulEviction = WarrantReturnValidator.isSuccessfulEviction(details);

        warrantReturnRepository.save(WarrantReturnEntity.builder()
            .enforcementOrder(warrant)
            .returnType(details.getType())
            .outcome(finalReturn ? details.getOutcome() : null)
            .returnName(returnName)
            .evictionAt(successfulEviction ? details.getEvictionDateTime().atZone(UK_ZONE_ID).toInstant() : null)
            .additionalDetails(blankToNull(details.getAdditionalDetails()))
            .recordedAt(Instant.now(utcClock))
            .recordedBy(securityContextService.getCurrentUserId())
            .build());

        returnName.statusAfterReturn()
            .filter(target -> target != warrant.getStatus())
            .ifPresent(target -> enforcementStatusTransitions.transition(warrant, target));

        EnforcementStatus statusAfterReturn = warrant.getStatus();
        if (statusAfterReturn == CLOSED || statusAfterReturn == AWAITING_BAILIFF_ALLOCATION) {
            // A closed warrant has no bailiff, and a reopened one is waiting for a new allocation.
            bailiffAllocationService.endActiveAllocations(warrant);
        }
        if (statusAfterReturn == CLOSED) {
            // TODO HDPI-8189: end the allocated bailiff's "allocated-bailiff" AM case role (end date today).
            // The allocation row itself is ended above; pcs-api cannot write AM case roles yet, see
            // BailiffAllocationService. Cancelling the enforcement's open bailiff tasks is HDPI-8627.
            log.info("Warrant {} closed by return {}", warrant.getId(), returnName);
        }
        return EnforcementChange.made(
            new RecordedWarrantReturn(returnName, warrant.getEnforcementNumber()));
    }

    /**
     * The current enforcement's most recent return, if it has one.
     */
    @Transactional(readOnly = true)
    public Optional<WarrantReturnEntity> findLatestReturn(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)
            .flatMap(enforcement -> warrantReturnRepository
                .findFirstByEnforcementOrderIdOrderByRecordedAtDesc(enforcement.getId()));
    }

    /**
     * One enforcement's most recent return, if it has one.
     */
    @Transactional(readOnly = true)
    public Optional<WarrantReturnEntity> findLatestReturn(EnforcementOrderEntity enforcement) {
        return warrantReturnRepository.findFirstByEnforcementOrderIdOrderByRecordedAtDesc(enforcement.getId());
    }

    /**
     * HDPI-8424 fee waiver hook: whether a further warrant applied for today would be free. It is free when
     * the claim's most recent final return is one of {@link #FREE_REPEAT_WARRANT_RETURN_NAMES} and today is
     * within {@value #FREE_REPEAT_WARRANT_MONTHS} calendar months of that warrant being generated. A warrant
     * is generated when a caseworker approves the application, which is what enf_case.approved_at records.
     *
     * <p>Not wired into the enforcement fee yet: see EnforcementOrderService#isPaymentOutstanding.</p>
     */
    @Transactional(readOnly = true)
    public boolean isNextWarrantFree(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity == null ? null : pcsCaseEntity.getClaims();
        if (claims == null || claims.isEmpty()) {
            return false;
        }
        return warrantReturnRepository.findByEnforcementOrderClaimIdOrderByRecordedAtDesc(claims.getFirst().getId())
            .stream()
            .filter(warrantReturn -> warrantReturn.getReturnType() == WarrantReturnType.FINAL)
            .findFirst()
            .map(lastFinal -> isFreeRepeatWarrant(lastFinal.getReturnName(),
                                                  lastFinal.getEnforcementOrder().getApprovedAt(),
                                                  LocalDate.now(ukClock)))
            .orElse(false);
    }

    static boolean isFreeRepeatWarrant(WarrantReturnName lastFinalReturnName, Instant warrantGeneratedAt,
                                       LocalDate applicationDate) {
        if (!FREE_REPEAT_WARRANT_RETURN_NAMES.contains(lastFinalReturnName) || warrantGeneratedAt == null) {
            return false;
        }
        LocalDate generated = warrantGeneratedAt.atZone(UK_ZONE_ID).toLocalDate();
        return !applicationDate.isAfter(generated.plusMonths(FREE_REPEAT_WARRANT_MONTHS));
    }

    static boolean isWarrant(EnforcementOrderEntity enforcement) {
        return enforcementType(enforcement.getEnforcementOrder()).filter(WARRANT_TYPES::contains).isPresent();
    }

    private static Optional<SelectEnforcementType> enforcementType(EnforcementOrder order) {
        if (order == null) {
            return Optional.empty();
        }
        String code = order.getChooseEnforcementType() == null ? null : order.getChooseEnforcementType().getValueCode();
        if (code == null) {
            return Optional.ofNullable(order.getSelectEnforcementType());
        }
        return Arrays.stream(SelectEnforcementType.values()).filter(type -> type.name().equals(code)).findFirst();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
