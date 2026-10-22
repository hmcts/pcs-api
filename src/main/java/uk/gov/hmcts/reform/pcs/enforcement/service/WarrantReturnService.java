package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PROPERTY_VISIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

@Slf4j
@Service
public class WarrantReturnService {

    static final Set<SelectEnforcementType> WARRANT_TYPES = Collections.unmodifiableSet(
        EnumSet.of(SelectEnforcementType.WARRANT, SelectEnforcementType.WARRANT_OF_RESTITUTION));

    static final Set<EnforcementStatus> RECORDABLE_STATUSES = Collections.unmodifiableSet(EnumSet.of(
        AWAITING_BAILIFF_ALLOCATION, AWAITING_SCHEDULING, AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED,
        CLAIMANT_UNABLE_TO_ATTEND, AWAITING_PROPERTY_VISIT, BREATHING_SPACE, CLOSED));

    private final CurrentEnforcementService currentEnforcementService;
    private final WarrantReturnRepository warrantReturnRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final WarrantReturnValidator warrantReturnValidator;
    private final SecurityContextService securityContextService;
    private final BailiffAllocationService bailiffAllocationService;
    private final EnforcementWaTaskService enforcementWaTaskService;
    private final Clock utcClock;

    public WarrantReturnService(CurrentEnforcementService currentEnforcementService,
                                WarrantReturnRepository warrantReturnRepository,
                                EnforcementStatusTransitions enforcementStatusTransitions,
                                WarrantReturnValidator warrantReturnValidator,
                                SecurityContextService securityContextService,
                                BailiffAllocationService bailiffAllocationService,
                                EnforcementWaTaskService enforcementWaTaskService,
                                @Qualifier("utcClock") Clock utcClock) {
        this.currentEnforcementService = currentEnforcementService;
        this.warrantReturnRepository = warrantReturnRepository;
        this.enforcementStatusTransitions = enforcementStatusTransitions;
        this.warrantReturnValidator = warrantReturnValidator;
        this.securityContextService = securityContextService;
        this.bailiffAllocationService = bailiffAllocationService;
        this.enforcementWaTaskService = enforcementWaTaskService;
        this.utcClock = utcClock;
    }

    /**
     * Throws {@link NoEnforcementException} if no warrant has been generated on the claim.
     */
    @Transactional(readOnly = true)
    public EnforcementOrderEntity findWarrant(long caseReference) {
        return requireWarrant(caseReference);
    }

    /**
     * The same read as {@link #findWarrant}, for callers inside the service, which would otherwise go round
     * the proxy and read with no transaction.
     */
    private EnforcementOrderEntity requireWarrant(long caseReference) {
        EnforcementOrderEntity current = currentEnforcementService.findCurrentEnforcement(caseReference).orElse(null);
        if (!hasGeneratedWarrant(current)) {
            throw new NoEnforcementException();
        }
        return current;
    }

    private EnforcementChange<EnforcementOrderEntity> findRecordableWarrant(long caseReference) {
        EnforcementOrderEntity current = currentEnforcementService.findCurrentEnforcement(caseReference).orElse(null);
        return hasGeneratedWarrant(current)
            ? EnforcementChange.made(current)
            : EnforcementChange.refused(NoEnforcementException.MESSAGE);
    }

    private static boolean hasGeneratedWarrant(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement != null
            && isWarrant(currentEnforcement)
            && RECORDABLE_STATUSES.contains(currentEnforcement.getStatus());
    }

    @Transactional(readOnly = true)
    public List<String> validate(long caseReference, WarrantReturnDetails details) {
        return warrantReturnValidator.validate(details, requireWarrant(caseReference).getStatus());
    }

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

        WarrantReturnEntity savedReturn = warrantReturnRepository.save(WarrantReturnEntity.builder()
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
            bailiffAllocationService.endActiveAllocations(warrant);
        }
        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            caseReference, "the warrant's task changes for this return",
            () -> enforcementWaTaskService.warrantReturnRecorded(caseReference, warrant.getId(),
                                                                 savedReturn.getId(), warrant.getBailiffDate(),
                                                                 statusAfterReturn));
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        if (statusAfterReturn == CLOSED) {
            // TODO HDPI-8189: end the allocated bailiff's "allocated-bailiff" AM case role (end date today).
            log.info("Warrant {} closed by return {}", warrant.getId(), returnName);
        }
        return EnforcementChange.made(
            new RecordedWarrantReturn(returnName, warrant.getEnforcementNumber()));
    }

    @Transactional(readOnly = true)
    public Optional<WarrantReturnEntity> findLatestReturn(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)
            .flatMap(this::findLatestReturn);
    }

    @Transactional(readOnly = true)
    public Optional<WarrantReturnEntity> findLatestReturn(EnforcementOrderEntity enforcement) {
        return EnforcementCaseViewLoads.latestReturns(claimIdOf(enforcement), this::loadLatestReturns)
            .map(latestReturns -> Optional.ofNullable(latestReturns.get(enforcement.getId())))
            .orElseGet(() -> warrantReturnRepository
                .findFirstByEnforcementOrderIdOrderByRecordedAtDesc(enforcement.getId()));
    }

    private Map<UUID, WarrantReturnEntity> loadLatestReturns(UUID claimId) {
        Map<UUID, WarrantReturnEntity> latestReturns = new HashMap<>();
        warrantReturnRepository.findByEnforcementOrderClaimIdOrderByRecordedAtDesc(claimId)
            .forEach(warrantReturn ->
                         latestReturns.putIfAbsent(warrantReturn.getEnforcementOrder().getId(), warrantReturn));
        return latestReturns;
    }

    private static UUID claimIdOf(EnforcementOrderEntity enforcement) {
        return enforcement.getClaim() == null ? null : enforcement.getClaim().getId();
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
