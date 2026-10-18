package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The eviction risk level of the claim's current warrant (HDPI-7343). Bailiffs set it once the warrant has
 * been approved, and can reset it while the warrant is live.
 */
@Service
@RequiredArgsConstructor
public class RiskLevelService {

    static final String NOT_AVAILABLE_MESSAGE = "You can only set the risk level of a warrant that has been "
        + "approved and is still live";

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final Clock utcClock;

    /**
     * Whether bailiff staff can set the risk level of the claim's current enforcement: it is an approved, live
     * warrant. "Set risk level" is offered on this, and its start and submit refuse on it.
     *
     * @param currentEnforcement the claim's current enforcement, or null if it has none
     */
    public static boolean canSetRiskLevel(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement != null && BailiffEnforcementRules.isApprovedLiveWarrant(currentEnforcement);
    }

    /**
     * The claim's current enforcement, if bailiffs can set its risk level. For a start callback: it refuses by
     * throwing, which
     * {@link uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementCallbackExceptionHandler} turns into an
     * error on the start response.
     *
     * @throws NoEnforcementException if the claim has no enforcement
     * @throws EnforcementEventNotAvailableException if the enforcement is not an approved, live warrant
     */
    public EnforcementOrderEntity getEnforcementForRiskLevel(long caseReference) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .orElseThrow(NoEnforcementException::new);
        if (!canSetRiskLevel(enforcement)) {
            throw new EnforcementEventNotAvailableException(NOT_AVAILABLE_MESSAGE);
        }
        return enforcement;
    }

    /**
     * Saves the risk level. A medium level has no reason, so any earlier reason is removed.
     *
     * @return made, with the warrant's number (null for a warrant that has none), or the errors to show when
     *     the claim has no approved, live warrant
     * @throws IllegalArgumentException if the level is missing, or is high without a reason
     */
    @Transactional
    public EnforcementChange<String> setRiskLevel(long caseReference, RiskLevel riskLevel, String reason) {
        Objects.requireNonNull(riskLevel, "riskLevel");
        boolean high = riskLevel == RiskLevel.HIGH;
        if (high && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A high risk level needs a reason");
        }
        EnforcementChange<EnforcementOrderEntity> settable = findEnforcementForRiskLevel(caseReference);
        if (settable.refused()) {
            return EnforcementChange.refused(settable.errors());
        }
        EnforcementOrderEntity enforcement = settable.change();
        enforcement.setRiskLevel(riskLevel);
        enforcement.setRiskLevelReason(high ? reason : null);
        enforcement.setRiskLevelSetAt(Instant.now(utcClock));
        enforcementOrderRepository.save(enforcement);
        // "Review risks and set level" and "Review updated risk information" are both closed by XUI through
        // the task-completion DMN, which has a row for each against this event.
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

    /**
     * The claim's current enforcement if bailiffs can set its risk level, or why they cannot.
     */
    private EnforcementChange<EnforcementOrderEntity> findEnforcementForRiskLevel(long caseReference) {
        Optional<EnforcementOrderEntity> current = currentEnforcementService.findCurrentEnforcement(caseReference);
        if (current.isEmpty()) {
            return EnforcementChange.refused(NoEnforcementException.MESSAGE);
        }
        if (!canSetRiskLevel(current.get())) {
            return EnforcementChange.refused(NOT_AVAILABLE_MESSAGE);
        }
        return EnforcementChange.made(current.get());
    }

    /**
     * The claim's current enforcement, for showing its risk level on the case.
     */
    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity);
    }
}
