package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;

@Component
@AllArgsConstructor
public class StatementOfTruthMapper {

    private final StatementOfTruthService statementOfTruthService;

    public void mapStatementOfTruthForWarrantRest(EnforcementOrder enforcementOrder,
                                                      EnforcementOrderEntity enforcementOrderEntity) {
        if (enforcementOrder.getRawWarrantRestDetails() != null
                && enforcementOrder.getRawWarrantRestDetails().getStatementOfTruthWarrantRest() != null) {
            StatementOfTruthEntity statementOfTruthEntity = statementOfTruthService.createStatementOfTruth(
                    enforcementOrder.getRawWarrantRestDetails().getStatementOfTruthWarrantRest());
            enforcementOrderEntity.setStatementOfTruth(statementOfTruthEntity);
        }
    }
}
