package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantOfRestitutionMapper;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final RiskProfileService riskProfileService;
    private final WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final DocumentService documentService;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        riskProfileService.processRisk(enforcementOrder, enforcementOrderEntity);
        processWarrantOfRestitution(enforcementOrder, enforcementOrderEntity);
        processDocuments(enforcementOrder, enforcementOrderEntity);
    }

    private void processWarrantOfRestitution(EnforcementOrder enforcementOrder,
                                             EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = warrantOfRestitutionMapper.toEntity(enforcementOrder,
                enforcementOrderEntity);
        WarrantOfRestitutionEntity saved = warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
        enforcementOrderEntity.setWarrantOfRestitutionDetails(saved);
    }

    private void processDocuments(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        if (enforcementOrder.getWarrantOfRestitutionDetails() != null
                && !CollectionUtils.isEmpty(
                        enforcementOrder.getWarrantOfRestitutionDetails().getAdditionalDocuments())) {
            List<DocumentEntity> documentEntities = documentService.createAllDocuments(enforcementOrder);
            enforcementOrderEntity.addDocuments(documentEntities);
            enforcementOrderRepository.save(enforcementOrderEntity);
        }
    }
}
