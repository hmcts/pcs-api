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
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementSelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrantofrestitution.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementRiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementSelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementWarrantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrantofrestitution.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementRiskProfileMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementWarrantMapper;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementRiskProfileRepository enforcementRiskProfileRepository;
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final EnforcementRiskProfileMapper enforcementRiskProfileMapper;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementSelectedDefendantRepository enforcementSelectedDefendantRepository;
    private final SelectedDefendantsMapper selectedDefendantsMapper;
    private final EnforcementWarrantMapper enforcementWarrantMapper;
    private final EnforcementWarrantRepository enforcementWarrantRepository;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        Set<EnforcementOrderEntity> enforcementEntitySet = claimEntity.getEnforcementOrders();
        if (CollectionUtils.isEmpty(enforcementEntitySet)) {
            return null;
        }

        return enforcementEntitySet
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder)
                .filter(order ->
                        order.getSelectEnforcementType().getValue().getCode().equals(enforcementType.name()))
                .findFirst()
                .orElse(null);
    }

    ClaimEntity retrieveClaimEntity(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();

        // Assuming 1 claim per PcsCase
        return claimEntities.getFirst();
    }

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(retrieveClaimEntity(pcsCaseEntity));
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);

        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcementOrderEntity);
        if (SelectEnforcementType.WARRANT.name().equals(enforcementOrder.getSelectEnforcementType().getValueCode())
                && enforcementOrder.getWarrantDetails() != null) {
            EnforcementRiskProfileEntity riskProfile =
                    enforcementRiskProfileMapper.toEntity(enforcementOrderEntity, enforcementOrder);
            enforcementRiskProfileRepository.save(riskProfile);
            storeWarrant(enforcementOrder, saved);
        } else if (SelectEnforcementType.WARRANT_OF_RESTITUTION.name()
                .equals(enforcementOrder.getSelectEnforcementType().getValueCode())) {
            createWarrantOfRestitution(enforcementOrderEntity);
        }
        createSelectedDefendants(enforcementOrderEntity);
    }

    private void createSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        List<EnforcementSelectedDefendantEntity> selectedDefendantsEntities =
                selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            enforcementSelectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
    }

    private void storeWarrant(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        EnforcementWarrantEntity warrantEntity = enforcementWarrantMapper.toEntity(enforcementOrder,
                enforcementOrderEntity);
        EnforcementWarrantEntity saved = enforcementWarrantRepository.save(warrantEntity);
        enforcementOrderEntity.setWarrantDetails(saved);
    }

    private void createWarrantOfRestitution(EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = new WarrantOfRestitutionEntity();
        warrantOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
    }
}
