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

    /**
     * Every type the claim has applied for, whatever became of the application. "Enforce the order" offers a
     * restitution type on this: it is what the claimant has already applied for that decides whether they can
     * apply to restore possession.
     */
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

    /**
     * The case's claim. Enforcement assumes one claim per case, as the rest of the service does.
     *
     * @throws ClaimNotFoundException if the case has no claim
     */
    ClaimEntity retrieveClaimEntity(PcsCaseEntity pcsCaseEntity) {
        return Optional.ofNullable(pcsCaseEntity.getMainClaim())
                .orElseThrow(() -> new ClaimNotFoundException(pcsCaseEntity.getCaseReference()));
    }

    /**
     * Whether the claimant can apply for a new enforcement: the claim's current enforcement, if it has one, is
     * not live (closed, rejected or transferred to the High Court). A claim can have at most one live
     * enforcement, and the current one is the live one if there is one. "Enforce the order" and "Resume draft
     * application" are offered on this, and their start and submit refuse on it.
     *
     * @param currentEnforcement the claim's current enforcement, or null if it has none
     */
    public static boolean canApplyForEnforcement(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement == null || !currentEnforcement.getStatus().isLive();
    }

    /**
     * Whether the case's claim has a live enforcement, i.e. one that is not closed, rejected or transferred
     * to the High Court. A claim can have at most one.
     */
    public boolean hasLiveEnforcement(long caseReference) {
        return !canApplyForEnforcement(currentEnforcementService.findCurrentEnforcement(caseReference).orElse(null));
    }

    /**
     * Saves the submitted application and deletes the draft. An application that owes a fee is saved awaiting
     * payment, and a Payments service request is raised for the fee once the transaction commits.
     *
     * @return a change that was made, carrying the fee the application owes or null when it owes nothing, or
     *     the errors to show the claimant instead
     * @throws FeeNotFoundException if the application owes a fee that the Fees Register cannot give, before
     *     anything is saved
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
        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcementOrder);
        // Decided before the new application is saved, so it only sees the claim's earlier enforcements.
        boolean paymentOutstanding = isPaymentOutstanding(claimEntity, enforcementType);
        final Optional<FeeDetails> feeOwed = paymentOutstanding
            ? Optional.of(enforcementFeeService.lookUpFee(enforcementType))
            : Optional.empty();

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, claimEntity);
        enforcementStatusTransitions.transition(enforcementOrderEntity,
            EnforcementStatusTransitions.initialStatus(paymentOutstanding));
        enforcementOrderEntity.setSubmittedAt(enforcementOrderEntity.getStatusUpdatedAt());
        // The writ, writ of restitution and warrant of restitution strategies each save a child row whose
        // enf_case_id is not nullable, and EnforcementOrderEntity has no association to cascade through, so the
        // enforcement has to be persistent before a strategy runs. The insert is flushed here so that the
        // one-live-enforcement index refuses a second application where the submit can turn it into a refusal,
        // rather than at commit.
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

    /**
     * What an application reaching awaiting review does, whichever path took it there: the claimant is told it
     * has been received, the application documents are generated, and a caseworker gets a task to review it.
     * Both callers run in a transaction, so all three go with the application or not at all.
     */
    private void applicationAwaitingReview(long caseReference, UUID enforcementId,
                                           SelectEnforcementType enforcementType) {
        enforcementNotificationService.notifyClaimantOfApplicationReceived(caseReference, enforcementType);
        enforcementDocumentScheduler.scheduleApplicationDocuments(enforcementId, enforcementType);
        enforcementWaTaskService.applicationAwaitingReview(caseReference, enforcementId);
    }

    /**
     * An application for a type with a fee owes it when it is submitted, because the fee is not taken during
     * submit, unless it is a free repeat warrant ({@link RepeatWarrantFeeWaiver}, HDPI-8424). An application for
     * a type with no fee, which is either restitution type, owes nothing. Either way an application that owes
     * nothing goes straight to review. The fee amounts on the submitted case data come from the client, so they
     * are not used to decide this.
     */
    private boolean isPaymentOutstanding(ClaimEntity claimEntity, SelectEnforcementType enforcementType) {
        return EnforcementApplicationFee.isPayable(enforcementType)
            && !repeatWarrantFeeWaiver.isFeeWaived(claimEntity.getId(), enforcementType);
    }

    /**
     * Moves a paid application on to awaiting review, tells the claimant it has been received and generates the
     * application documents, which the features describe as produced once the claimant has submitted and paid.
     * {@link EnforcementPaymentCallbackHandler} calls this when Payments reports the fee paid.
     *
     * <p>A failure to queue the email or the documents is thrown here, unlike in the submit handlers, and
     * rolls the whole callback back: there is no user waiting on a screen to refuse, and the right answer to
     * Payments is a failure it will send again rather than a success for work that was not queued.</p>
     */
    void moveToAwaitingReview(long caseReference, EnforcementOrderEntity enforcement) {
        enforcementStatusTransitions.transition(enforcement, EnforcementStatus.AWAITING_REVIEW);
        enforcementOrderRepository.save(enforcement);
        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcement.getEnforcementOrder());
        applicationAwaitingReview(caseReference, enforcement.getId(), enforcementType);
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        // The status is held on the enf_case.status column and only copied onto the case view, and the list of
        // enforcements is built from the enf_case rows for the case view, so do not store a copy of either in the
        // enforcement order JSON.
        enforcementOrder.setEnforcementStatus(null);
        enforcementOrder.setEnforcements(null);
        // Bailiff notes are held on enf_bailiff_note, so do not copy the case view's notes into the JSON either.
        enforcementOrder.setBailiffNotes(null);
        enforcementOrder.setBailiffNoteText(null);
        // Warrant returns are held on enf_warrant_return.
        enforcementOrder.setWarrantReturn(null);
        enforcementOrder.setLatestWarrantReturn(null);
        // The tab markdown is rendered for the case view from the enforcement records.
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
     * The application as it will read back out of the enforcement_order JSON column. Hibernate dirty-checks that
     * column against a JSON round trip of the value it holds, and reading an order materialises every
     * {@link com.fasterxml.jackson.annotation.JsonUnwrapped} nested object the application left null, so storing
     * the submitted order as built would make the row dirty the moment it was inserted and cost a second write.
     */
    private EnforcementOrder storedForm(EnforcementOrder enforcementOrder) {
        return objectMapper.convertValue(enforcementOrder, EnforcementOrder.class);
    }
}
