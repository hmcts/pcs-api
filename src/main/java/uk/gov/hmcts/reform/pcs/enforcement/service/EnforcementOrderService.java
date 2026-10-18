package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementApplicationFee;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final RepeatWarrantFeeWaiver repeatWarrantFeeWaiver;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        List<EnforcementOrderEntity> enforcementEntities =
                enforcementOrderRepository.findByClaimId(claimEntity.getId());

        return enforcementEntities
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder)
                .filter(order -> SelectEnforcementType.from(order) == enforcementType)
                .findFirst()
                .orElse(null);
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
     * Whether the case's claim has a live enforcement, i.e. one that is not closed, rejected or transferred
     * to the High Court. A claim can have at most one.
     */
    public boolean hasLiveEnforcement(long caseReference) {
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseService.loadCase(caseReference));
        return enforcementOrderRepository.existsByClaimIdAndStatusNotIn(
            claimEntity.getId(), EnforcementStatus.notLive());
    }

    /**
     * Saves the submitted application and deletes the draft.
     *
     * @return whether the application owes a fee, i.e. whether it was saved awaiting payment
     */
    @Transactional
    public boolean saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        boolean paymentOutstanding = createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
        return paymentOutstanding;
    }

    private boolean createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcementOrder);
        // Decided before the new application is saved, so it only sees the claim's earlier enforcements.
        boolean paymentOutstanding = isPaymentOutstanding(claimEntity, enforcementType);

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, claimEntity);
        enforcementStatusTransitions.transition(enforcementOrderEntity,
            EnforcementStatusTransitions.initialStatus(paymentOutstanding));
        // The writ, writ of restitution and warrant of restitution strategies each save a child row whose
        // enf_case_id is not nullable, and EnforcementOrderEntity has no association to cascade through, so the
        // enforcement has to be persistent before a strategy runs.
        EnforcementOrderEntity savedEntity = enforcementOrderRepository.save(enforcementOrderEntity);
        strategyFactory.getStrategy(enforcementType).process(savedEntity, enforcementOrder);
        if (!paymentOutstanding && EnforcementApplicationFee.isPayable(enforcementType)) {
            log.info("Enforcement {} on case {} is a free repeat warrant", savedEntity.getId(), caseReference);
        }
        return paymentOutstanding;
    }

    /**
     * An application for a type with a fee owes it when it is submitted, because the fee is not taken during
     * submit, unless it is a free repeat warrant ({@link RepeatWarrantFeeWaiver}, HDPI-8424). An application for
     * a type with no fee, which is either restitution type, owes nothing. Either way an application that owes
     * nothing goes straight to review. The fee amounts on the submitted case data come from the client, so they
     * are not used to decide this.
     *
     * <p>TODO HDPI-2877: nothing moves an application on from AWAITING_PAYMENT yet, because pcs-api has no
     * enforcement payment callback on this branch. When the enforcement payment handler from hmcts/pcs-api#2941
     * lands, its PAID path should call {@code enforcementStatusTransitions.transition(entity, AWAITING_REVIEW)}
     * from inside this package (it must link the fee payment to the enf_case id to find the entity).
     */
    private boolean isPaymentOutstanding(ClaimEntity claimEntity, SelectEnforcementType enforcementType) {
        return EnforcementApplicationFee.isPayable(enforcementType)
            && !repeatWarrantFeeWaiver.isFeeWaived(claimEntity.getId(), enforcementType);
    }

    /**
     * Moves the claim's application that is awaiting payment on to awaiting review, as paying the fee will.
     * Only the enforcement testing-support endpoint calls this until the payment handler lands (HDPI-2877).
     *
     * @return whether an application was awaiting payment
     */
    @Transactional
    public boolean recordFeePaid(long caseReference) {
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseService.loadCase(caseReference));
        return enforcementOrderRepository
            .findByClaimIdAndStatus(claimEntity.getId(), EnforcementStatus.AWAITING_PAYMENT)
            .stream()
            .findFirst()
            .map(enforcement -> {
                enforcementStatusTransitions.transition(enforcement, EnforcementStatus.AWAITING_REVIEW);
                enforcementOrderRepository.save(enforcement);
                return true;
            })
            .orElse(false);
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
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }
}
