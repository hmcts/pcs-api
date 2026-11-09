package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantDetailsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.SelectedDefendantsMapper;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantStrategy implements EnforcementTypeStrategy {

    private final RiskProfileService riskProfileService;
    private final StatementOfTruthService statementOfTruthService;
    private final SelectedDefendantsMapper selectedDefendantsMapper;
    private final WarrantDetailsMapper warrantDetailsMapper;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        processWarrant(enforcementOrder, enforcementOrderEntity);
        riskProfileService.processRisk(enforcementOrder, enforcementOrderEntity);
        statementOfTruthService.processStatementOfTruth(enforcementOrder.getWarrantDetails().getStatementOfTruth(),
                enforcementOrderEntity);
        processSelectedDefendants(enforcementOrderEntity);
    }

    private void processWarrant(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        WarrantEntity warrantEntity = warrantDetailsMapper.toEntity(enforcementOrder,
                                                                    enforcementOrderEntity);
        enforcementOrderEntity.setWarrantDetails(warrantEntity);
    }

    private void processSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        List<SelectedDefendantEntity> selectedDefendantsEntities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);
        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            enforcementOrderEntity.addSelectedDefendants(selectedDefendantsEntities);
        }
    }

}
