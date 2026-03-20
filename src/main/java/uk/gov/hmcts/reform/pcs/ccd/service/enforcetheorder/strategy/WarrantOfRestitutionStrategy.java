package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.StatementOfTruthMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantOfRestitutionMapper;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final StatementOfTruthMapper statementOfTruthMapper;
    private final WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        processWarrantOfRestitution(enforcementOrder, enforcementOrderEntity);
    }

    private void processWarrantOfRestitution(EnforcementOrder enforcementOrder,
                                             EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = warrantOfRestitutionMapper.toEntity(enforcementOrder,
                enforcementOrderEntity);
        statementOfTruthMapper.mapStatementOfTruthForWarrantRest(enforcementOrder, enforcementOrderEntity);
        warrantOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);

        WarrantOfRestitutionEntity saved = warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
        enforcementOrderEntity.setWarrantOfRestitutionDetails(saved);
    }
}
