package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDocumentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatementOfTruthService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskProfileService;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.WarrantOfRestitutionMapper;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final RiskProfileService riskProfileService;
    private final WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementDocumentService enforcementDocumentService;
    private final EnforcementStatementOfTruthService enforcementStatementOfTruthService;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        riskProfileService.processRisk(enforcementOrder, enforcementOrderEntity);
        processWarrantOfRestitution(enforcementOrder, enforcementOrderEntity);
        processStatementOfTruth(enforcementOrder, enforcementOrderEntity);
        processDocuments(enforcementOrder, enforcementOrderEntity);
    }

    /**
     * The journey signs the warrant journey's statement of truth page, so the answers are on
     * {@link WarrantDetails} (HDPI-7555).
     */
    private void processStatementOfTruth(EnforcementOrder enforcementOrder,
                                         EnforcementOrderEntity enforcementOrderEntity) {
        WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
        enforcementStatementOfTruthService.processStatementOfTruth(
                warrantDetails == null ? null : warrantDetails.getStatementOfTruth(), enforcementOrderEntity);
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
            enforcementDocumentService.createAllDocuments(enforcementOrderEntity, enforcementOrder);
            enforcementOrderRepository.save(enforcementOrderEntity);
        }
    }
}
