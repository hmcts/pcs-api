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

@Service
@RequiredArgsConstructor
public class RiskLevelService {

    static final String NOT_AVAILABLE_MESSAGE = "You can only set the risk level of a warrant that has been "
        + "approved and is still live";

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final Clock utcClock;

    public static boolean canSetRiskLevel(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement != null && BailiffEnforcementRules.isApprovedLiveWarrant(currentEnforcement);
    }

    public EnforcementOrderEntity getEnforcementForRiskLevel(long caseReference) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .orElseThrow(NoEnforcementException::new);
        if (!canSetRiskLevel(enforcement)) {
            throw new EnforcementEventNotAvailableException(NOT_AVAILABLE_MESSAGE);
        }
        return enforcement;
    }

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
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

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

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity);
    }
}
