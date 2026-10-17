package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

/**
 * Records a caseworker's decision on the claim's enforcement application that is awaiting review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementDecisionService {

    public static final String NO_REJECTION_REASON_MESSAGE =
        "A reason is needed to reject an enforcement application";

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final EnforcementNumberGenerator enforcementNumberGenerator;

    public boolean hasEnforcementAwaitingReview(long caseReference) {
        return findAwaitingReview(caseReference).isPresent();
    }

    /**
     * Approves the application: a warrant goes on to bailiff allocation, a writ is transferred to the High Court.
     * Gives it a warrant or writ number and records when it was approved.
     *
     * @throws NoEnforcementAwaitingReviewException if no application is awaiting review
     */
    @Transactional
    public EnforcementDecisionOutcome accept(long caseReference) {
        EnforcementOrderEntity enforcement = awaitingReview(caseReference);
        SelectEnforcementType enforcementType = enforcementTypeOf(enforcement);

        enforcementStatusTransitions.transition(enforcement, approvedStatus(enforcementType));
        enforcement.setApprovedAt(enforcement.getStatusUpdatedAt());
        String enforcementNumber = enforcementNumberGenerator.next(enforcementType);
        enforcement.setEnforcementNumber(enforcementNumber);

        // TODO HDPI-9566: generate the sealed warrant (county court seal, internal users only) or writ (High Court
        //  seal, claimant and internal users), in Welsh when the claimant asked for Welsh documents, and store it
        //  in the "Enforcement" folder. The Docmosis templates are not in pcs-api yet, so no document is made here.
        // TODO HDPI-7538: email the claimant ("Caseworker generates the warrant of possession" / "Application for a
        //  writ successful"). Enforcement has no Notify templates yet.
        enforcementOrderRepository.save(enforcement);
        log.info("Enforcement {} on case {} approved as {}", enforcement.getId(), caseReference, enforcementNumber);
        return new EnforcementDecisionOutcome(enforcementType, EnforcementDecision.ACCEPT, enforcementNumber);
    }

    /**
     * Rejects the application. No number is given, and the claimant can apply again because a rejected
     * enforcement is not live.
     *
     * @throws NoEnforcementAwaitingReviewException if no application is awaiting review
     * @throws EnforcementEventNotAvailableException if no rejection reason was given
     */
    @Transactional
    public EnforcementDecisionOutcome reject(long caseReference, String rejectionReason) {
        if (StringUtils.isBlank(rejectionReason)) {
            throw new EnforcementEventNotAvailableException(NO_REJECTION_REASON_MESSAGE);
        }
        EnforcementOrderEntity enforcement = awaitingReview(caseReference);
        final SelectEnforcementType enforcementType = enforcementTypeOf(enforcement);

        enforcementStatusTransitions.transition(enforcement, REJECTED);
        enforcement.setRejectionReason(rejectionReason.trim());

        // TODO HDPI-7538: email the claimant ("Caseworker rejects the warrant of possession application").
        //  Enforcement has no Notify templates yet.
        enforcementOrderRepository.save(enforcement);
        log.info("Enforcement {} on case {} rejected", enforcement.getId(), caseReference);
        return new EnforcementDecisionOutcome(enforcementType, EnforcementDecision.REJECT, null);
    }

    static EnforcementStatus approvedStatus(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT, WARRANT_OF_RESTITUTION -> AWAITING_BAILIFF_ALLOCATION;
            case WRIT, WRIT_OF_RESTITUTION -> TRANSFERRED_TO_HIGH_COURT;
        };
    }

    private EnforcementOrderEntity awaitingReview(long caseReference) {
        return findAwaitingReview(caseReference).orElseThrow(NoEnforcementAwaitingReviewException::new);
    }

    private Optional<EnforcementOrderEntity> findAwaitingReview(long caseReference) {
        List<ClaimEntity> claims = pcsCaseService.loadCase(caseReference).getClaims();
        if (claims == null || claims.isEmpty()) {
            return Optional.empty();
        }
        // A claim has at most one live enforcement, so at most one awaiting review.
        return enforcementOrderRepository.findByClaimIdAndStatus(claims.getFirst().getId(), AWAITING_REVIEW)
            .stream()
            .findFirst();
    }

    private static SelectEnforcementType enforcementTypeOf(EnforcementOrderEntity enforcement) {
        EnforcementOrder enforcementOrder = enforcement.getEnforcementOrder();
        if (enforcementOrder != null && enforcementOrder.getChooseEnforcementType() != null
            && enforcementOrder.getChooseEnforcementType().getValueCode() != null) {
            return SelectEnforcementType.getSelectEnforcementTypeFromName(
                enforcementOrder.getChooseEnforcementType().getValueCode());
        }
        if (enforcementOrder != null && enforcementOrder.getSelectEnforcementType() != null) {
            return enforcementOrder.getSelectEnforcementType();
        }
        throw new IllegalStateException("Enforcement %s has no enforcement type".formatted(enforcement.getId()));
    }
}
