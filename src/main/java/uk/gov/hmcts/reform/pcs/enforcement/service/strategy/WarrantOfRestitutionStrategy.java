package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDocumentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskProfileService;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.WarrantOfRestitutionMapper;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final RiskProfileService riskProfileService;
    private final WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementDocumentService enforcementDocumentService;

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
            List<DocumentEntity> documentEntities = enforcementDocumentService.createAllDocuments(enforcementOrder);
            enforcementOrderEntity.addDocuments(documentEntities);
            enforcementOrderRepository.save(enforcementOrderEntity);
        }
    }
}
