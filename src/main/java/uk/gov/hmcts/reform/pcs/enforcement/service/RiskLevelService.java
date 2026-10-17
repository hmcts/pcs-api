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
     * The claim's current enforcement, if bailiffs can set its risk level.
     *
     * @throws NoEnforcementException if the claim has no enforcement
     * @throws EnforcementEventNotAvailableException if the enforcement is not an approved, live warrant
     */
    public EnforcementOrderEntity getEnforcementForRiskLevel(long caseReference) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .orElseThrow(NoEnforcementException::new);
        if (!BailiffEnforcementRules.isApprovedLiveWarrant(enforcement)) {
            throw new EnforcementEventNotAvailableException(NOT_AVAILABLE_MESSAGE);
        }
        return enforcement;
    }

    /**
     * Saves the risk level. A medium level has no reason, so any earlier reason is removed.
     *
     * @throws IllegalArgumentException if the level is missing, or is high without a reason
     */
    @Transactional
    public void setRiskLevel(long caseReference, RiskLevel riskLevel, String reason) {
        Objects.requireNonNull(riskLevel, "riskLevel");
        boolean high = riskLevel == RiskLevel.HIGH;
        if (high && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A high risk level needs a reason");
        }
        EnforcementOrderEntity enforcement = getEnforcementForRiskLevel(caseReference);
        enforcement.setRiskLevel(riskLevel);
        enforcement.setRiskLevelReason(high ? reason : null);
        enforcement.setRiskLevelSetAt(Instant.now(utcClock));
        enforcementOrderRepository.save(enforcement);
        // TODO(HDPI-8627): complete the "Review risks and set level" task.
    }

    /**
     * The claim's current enforcement, for showing its risk level on the case.
     */
    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity);
    }
}
