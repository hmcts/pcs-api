package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskAssessmentUpdateEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.RiskAssessmentUpdateRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.ClaimantRiskAnswersMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimantRiskAssessmentService {

    private final CurrentEnforcementService currentEnforcementService;
    private final RiskAssessmentUpdateRepository riskAssessmentUpdateRepository;
    private final ClaimantRiskAnswersMapper claimantRiskAnswersMapper;
    private final EnforcementWaTaskService enforcementWaTaskService;
    private final Clock utcClock;

    public Optional<ClaimantRiskAnswers> getOriginalAnswers(EnforcementOrderEntity enforcement) {
        return Optional.ofNullable(enforcement.getEnforcementOrder())
            .flatMap(claimantRiskAnswersMapper::fromApplication);
    }

    public Optional<RiskAssessmentUpdateEntity> getLatestUpdate(EnforcementOrderEntity enforcement) {
        if (enforcement.getId() == null) {
            return Optional.empty();
        }
        return riskAssessmentUpdateRepository.findFirstByEnforcementOrderIdOrderByUpdatedAtDesc(enforcement.getId());
    }

    public Optional<ClaimantRiskAnswers> getCurrentAnswers(EnforcementOrderEntity enforcement) {
        return getLatestUpdate(enforcement)
            .map(RiskAssessmentUpdateEntity::getAnswers)
            .or(() -> getOriginalAnswers(enforcement));
    }

    public void prefillAnswers(long caseReference, EnforcementOrder enforcementOrder) {
        findWarrant(caseReference)
            .flatMap(this::getCurrentAnswers)
            .ifPresentOrElse(
                answers -> claimantRiskAnswersMapper.applyToWarrantFields(answers, enforcementOrder),
                () -> claimantRiskAnswersMapper.clearWarrantFields(enforcementOrder));
    }

    @Transactional
    public EnforcementChange<Boolean> recordAnswers(long caseReference, EnforcementOrder submitted) {
        if (submitted == null) {
            return EnforcementChange.made(false);
        }
        ClaimantRiskAnswers answers = claimantRiskAnswersMapper.fromWarrantFields(submitted);
        if (answers.equals(new ClaimantRiskAnswers())) {
            log.info("Case {}: no risk assessment answers submitted", caseReference);
            return EnforcementChange.made(false);
        }
        Optional<EnforcementOrderEntity> warrant = findWarrant(caseReference);
        if (warrant.isEmpty()) {
            log.info("Case {}: no approved, live warrant, risk assessment answers not saved", caseReference);
            return EnforcementChange.made(false);
        }
        EnforcementOrderEntity enforcement = warrant.get();
        Optional<ClaimantRiskAnswers> current = getCurrentAnswers(enforcement);
        if (current.isEmpty() || current.get().normalised().equals(answers)) {
            return EnforcementChange.made(false);
        }
        RiskAssessmentUpdateEntity update = riskAssessmentUpdateRepository.save(RiskAssessmentUpdateEntity.builder()
            .enforcementOrder(enforcement)
            .answers(answers)
            .updatedAt(Instant.now(utcClock))
            .build());
        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            caseReference, "the bailiff's updated risk information task",
            () -> enforcementWaTaskService.riskAnswersChanged(caseReference, update.getId()));
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        return EnforcementChange.made(true);
    }

    private Optional<EnforcementOrderEntity> findWarrant(long caseReference) {
        return currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(BailiffEnforcementRules::isApprovedLiveWarrant);
    }
}
