package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.StatementOfTruthMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantOfRestitutionMapper;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final RiskProfileService riskProfileService;
    private final StatementOfTruthMapper statementOfTruthMapper;
    private final WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final StatementOfTruthRepository statementOfTruthRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        riskProfileService.processRisk(enforcementOrder, enforcementOrderEntity);
        processWarrantOfRestitution(enforcementOrder, enforcementOrderEntity);
        processStatementOfTruth(enforcementOrder, enforcementOrderEntity);
    }

    private void processWarrantOfRestitution(EnforcementOrder enforcementOrder,
                                             EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = warrantOfRestitutionMapper.toEntity(enforcementOrder,
                enforcementOrderEntity);
        warrantOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        WarrantOfRestitutionEntity saved = warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
        enforcementOrderEntity.setWarrantOfRestitutionDetails(saved);
    }

    private void processStatementOfTruth(EnforcementOrder enforcementOrder,
                                         EnforcementOrderEntity enforcementOrderEntity) {
        statementOfTruthMapper.mapStatementOfTruthForWarrantRest(enforcementOrder, enforcementOrderEntity);
        StatementOfTruthEntity statementOfTruthEntity = enforcementOrderEntity.getStatementOfTruth();
        if (statementOfTruthEntity != null) {
            statementOfTruthRepository.save(statementOfTruthEntity);
        }
    }
}
