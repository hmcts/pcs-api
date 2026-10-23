package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WritDetailsMapper;

@Component
@AllArgsConstructor
public class WritStrategy implements EnforcementTypeStrategy {

    private final WritDetailsMapper writDetailsMapper;
    private final StatementOfTruthService statementOfTruthService;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        processWrit(enforcementOrder, enforcementOrderEntity);
        statementOfTruthService.processStatementOfTruth(enforcementOrder.getWritDetails().getStatementOfTruth(),
                enforcementOrderEntity);
    }

    private void processWrit(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        WritEntity writEntity = writDetailsMapper.toEntity(enforcementOrder,
                enforcementOrderEntity);
        enforcementOrderEntity.setWritDetails(writEntity);
    }
}
