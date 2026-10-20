package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementApplicationFee;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final CurrentEnforcementService currentEnforcementService;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final RepeatWarrantFeeWaiver repeatWarrantFeeWaiver;
    private final EnforcementNotificationService enforcementNotificationService;
    private final EnforcementDocumentScheduler enforcementDocumentScheduler;
    private final EnforcementFeeService enforcementFeeService;
    private final EnforcementWaTaskService enforcementWaTaskService;
    private final ObjectMapper objectMapper;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        return claimApplications(caseReference)
                .filter(order -> SelectEnforcementType.from(order) == enforcementType)
                .findFirst()
                .orElse(null);
    }

    public Set<SelectEnforcementType> retrieveEnforcementTypes(long caseReference) {
        return claimApplications(caseReference)
                .map(SelectEnforcementType::from)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SelectEnforcementType.class)));
    }

    private Stream<EnforcementOrder> claimApplications(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        return enforcementOrderRepository.findByClaimId(claimEntity.getId())
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder);
    }

    ClaimEntity retrieveClaimEntity(PcsCaseEntity pcsCaseEntity) {
        return Optional.ofNullable(pcsCaseEntity.getMainClaim())
                .orElseThrow(() -> new ClaimNotFoundException(pcsCaseEntity.getCaseReference()));
    }

    /**
     * Whether the claimant can apply for a new enforcement, given the claim's current one or null for none.
     */
    public static boolean canApplyForEnforcement(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement == null || !currentEnforcement.getStatus().isLive();
    }

    public boolean hasLiveEnforcement(long caseReference) {
        return !canApplyForEnforcement(currentEnforcementService.findCurrentEnforcement(caseReference).orElse(null));
    }

    /**
     * Throws {@link FeeNotFoundException}, before anything is saved, if the fee owed cannot be looked up.
     */
    @Transactional
    public EnforcementChange<FeeDetails> saveAndClearDraftData(long caseReference,
                                                               EnforcementOrder enforcementOrder) {
        EnforcementChange<FeeDetails> submitted = createEnforcementOrder(caseReference, enforcementOrder);
        if (submitted.refused()) {
            return submitted;
        }
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
        return submitted;
    }

    private EnforcementChange<FeeDetails> createEnforcementOrder(long caseReference,
                                                                 EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        SelectEnforcementType enforcementType = Objects.requireNonNull(
            SelectEnforcementType.from(enforcementOrder), "The enforcement application records no type");
        // Decided before the new application is saved, so it only sees the claim's earlier enforcements.
        boolean paymentOutstanding = isPaymentOutstanding(claimEntity, enforcementType);
        final Optional<FeeDetails> feeOwed = paymentOutstanding
            ? Optional.of(enforcementFeeService.lookUpFee(enforcementType))
            : Optional.empty();

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, claimEntity);
        enforcementStatusTransitions.transition(enforcementOrderEntity,
            EnforcementStatusTransitions.initialStatus(paymentOutstanding));
        enforcementOrderEntity.setSubmittedAt(enforcementOrderEntity.getStatusUpdatedAt());
        // Flushed so the strategies below have a persistent row to hang their child rows off, and so the
        // one-live-enforcement index refuses a second application here rather than at commit.
        EnforcementOrderEntity savedEntity = enforcementOrderRepository.saveAndFlush(enforcementOrderEntity);
        strategyFactory.getStrategy(enforcementType).process(savedEntity, enforcementOrder);
        if (!paymentOutstanding && EnforcementApplicationFee.isPayable(enforcementType)) {
            log.info("Enforcement {} on case {} is a free repeat warrant", savedEntity.getId(), caseReference);
        }
        EnforcementChange<Void> scheduled;
        if (paymentOutstanding) {
            FeeDetails fee = feeOwed.orElseThrow();
            scheduled = EnforcementSideEffects.schedule(caseReference, "the application's fee payment request",
                () -> enforcementFeeService.requestPayment(pcsCaseEntity, savedEntity.getId(), fee));
        } else {
            scheduled = EnforcementSideEffects.schedule(
                caseReference, "the application documents, the claimant's email and the review task",
                () -> applicationAwaitingReview(caseReference, savedEntity.getId(), enforcementType));
        }
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        return EnforcementChange.made(feeOwed.orElse(null));
    }

    private void applicationAwaitingReview(long caseReference, UUID enforcementId,
                                           SelectEnforcementType enforcementType) {
        enforcementNotificationService.notifyClaimantOfApplicationReceived(caseReference, enforcementType);
        enforcementDocumentScheduler.scheduleApplicationDocuments(enforcementId, enforcementType);
        enforcementWaTaskService.applicationAwaitingReview(caseReference, enforcementId);
    }

    private boolean isPaymentOutstanding(ClaimEntity claimEntity, SelectEnforcementType enforcementType) {
        return EnforcementApplicationFee.isPayable(enforcementType)
            && !repeatWarrantFeeWaiver.isFeeWaived(claimEntity.getId(), enforcementType);
    }

    /**
     * A failure to queue the email or the documents is thrown rather than refused, so Payments retries.
     */
    void moveToAwaitingReview(long caseReference, EnforcementOrderEntity enforcement) {
        enforcementStatusTransitions.transition(enforcement, EnforcementStatus.AWAITING_REVIEW);
        enforcementOrderRepository.save(enforcement);
        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcement.getEnforcementOrder());
        applicationAwaitingReview(caseReference, enforcement.getId(), enforcementType);
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        // Each of these is held on its own table or rendered for the case view, not in this JSON column.
        enforcementOrder.setEnforcementStatus(null);
        enforcementOrder.setEnforcements(null);
        enforcementOrder.setBailiffNotes(null);
        enforcementOrder.setBailiffNoteText(null);
        enforcementOrder.setWarrantReturn(null);
        enforcementOrder.setLatestWarrantReturn(null);
        enforcementOrder.setEnforcementStatusTabMarkdown(null);
        enforcementOrder.setRiskAssessmentPrintMarkdown(null);
        enforcementOrder.setRiskAssessmentInternalMarkdown(null);
        enforcementOrder.setRiskAssessmentClaimantMarkdown(null);
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(storedForm(enforcementOrder));
        return enforcementOrderEntity;
    }

    /**
     * The order as it reads back out of the JSON column; storing it as built dirties the row on insert.
     */
    private EnforcementOrder storedForm(EnforcementOrder enforcementOrder) {
        return objectMapper.convertValue(enforcementOrder, EnforcementOrder.class);
    }
}
