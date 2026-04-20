package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.confirmeviction.EvictionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EvictionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.getSelectEnforcementTypeFromName;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;
    private final EvictionRepository evictionRepository;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        Set<EnforcementOrderEntity> enforcementEntitySet = getEnforcementOrderEntities(caseReference);
        return enforcementEntitySet
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder)
                .filter(order ->
                        order.getChooseEnforcementType().getValue().getCode().equals(enforcementType.name()))
                .findFirst()
                .orElse(null);
    }

    Set<EnforcementOrderEntity> getEnforcementOrderEntities(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        if (claimEntity == null) {
            log.debug("No claim entities found for PCS case when retrieving enforcement order for caseReference={}",
                      caseReference
            );
            return Set.of();
        }
        return claimEntity.getEnforcementOrders();
    }

    ClaimEntity retrieveClaimEntity(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();
        if (CollectionUtils.isEmpty(claimEntities)) {
            return null;
        }
        // Assuming 1 claim per PcsCase
        return claimEntities.getFirst();
    }

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        ClaimEntity claimEntity = getClaimEntity(caseReference);
        EnforcementOrderEntity orderEntity = enforcementOrderRepository
            .save(mapToEntity(enforcementOrder, claimEntity));
        strategyFactory.getStrategy(getSelectEnforcementTypeFromName(
            enforcementOrder.getChooseEnforcementType().getValueCode()))
            .process(orderEntity, enforcementOrder);
    }

    private ClaimEntity getClaimEntity(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();
        if (CollectionUtils.isEmpty(claimEntities)) {
            throw new IllegalStateException("Cannot create enforcement order because no claim entity exists for "
                                                + "caseReference=" + caseReference);
        }
        // Assuming 1 claim per PcsCase
        return claimEntities.getFirst();
    }

    public void confirmEviction(long caseReference) {
        EvictionEntity confirmEviction = mapToConfirmEvictionEntity(caseReference);
        evictionRepository.save(confirmEviction);
    }

    private EvictionEntity mapToConfirmEvictionEntity(long caseReference) {
        EvictionEntity confirmEviction = new EvictionEntity();
        EnforcementOrderEntity enforcementOrderEntity = retrieveEnforcementOrderEntity(caseReference);
        confirmEviction.setEnforcementOrder(enforcementOrderEntity);

        return confirmEviction;
    }

    private EnforcementOrderEntity retrieveEnforcementOrderEntity(long caseReference) {
        Set<EnforcementOrderEntity> enforcementOrders = getEnforcementOrderEntities(caseReference);
        if (enforcementOrders.isEmpty()) {
            return null;
        }
        return enforcementOrders.stream().findFirst().orElse(null);
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }

}
