package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;
    private final EnforcementStatusTransitions enforcementStatusTransitions;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        List<EnforcementOrderEntity> enforcementEntities =
                enforcementOrderRepository.findByClaimId(claimEntity.getId());

        return enforcementEntities
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder)
                .filter(order ->
                        order.getChooseEnforcementType().getValue().getCode().equals(enforcementType.name()))
                .findFirst()
                .orElse(null);
    }

    ClaimEntity retrieveClaimEntity(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();

        // Assuming 1 claim per PcsCase
        return claimEntities.getFirst();
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

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, claimEntity);
        enforcementStatusTransitions.transition(enforcementOrderEntity,
            EnforcementStatusTransitions.initialStatus(isPaymentOutstanding()));
        strategyFactory.getStrategy(SelectEnforcementType.getSelectEnforcementTypeFromName(
                        enforcementOrder.getChooseEnforcementType().getValueCode()))
                .process(enforcementOrderEntity, enforcementOrder);
        enforcementOrderRepository.save(enforcementOrderEntity);
    }

    /**
     * Every application owes a fee when it is submitted: no fee exemption exists yet (a free repeat warrant,
     * HDPI-8424, is still to be agreed) and the fee is not taken during submit. The fee amounts on the
     * submitted case data come from the client, so they are not used to decide this.
     *
     * <p>TODO HDPI-8424: WarrantReturnService#isNextWarrantFree answers whether a further warrant is free from
     * the return history. It is not used here until the fee exemption is agreed (OPEN-QUESTIONS 6).</p>
     *
     * <p>TODO HDPI-2877: nothing moves an application on from AWAITING_PAYMENT yet, because pcs-api has no
     * enforcement payment callback on this branch. When the enforcement payment handler from hmcts/pcs-api#2941
     * lands, its PAID path should call {@code enforcementStatusTransitions.transition(entity, AWAITING_REVIEW)}
     * from inside this package (it must link the fee payment to the enf_case id to find the entity).
     */
    private static boolean isPaymentOutstanding() {
        return true;
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        // The status is held on the enf_case.status column and only copied onto the case view, so do not
        // store a copy of it in the enforcement order JSON.
        enforcementOrder.setEnforcementStatus(null);
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
