package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.StatementOfTruthService;

@Component
@AllArgsConstructor
public class StatementOfTruthMapper {

    private final StatementOfTruthService statementOfTruthService;

    public StatementOfTruthEntity mapStatementOfTruthForWarrantRest(EnforcementOrder enforcementOrder) {
        if (enforcementOrder.getRawWarrantRestDetails() != null
                && enforcementOrder.getRawWarrantRestDetails().getStatementOfTruthWarrantRest() != null) {
            return statementOfTruthService.createStatementOfTruth(
                    enforcementOrder.getRawWarrantRestDetails().getStatementOfTruthWarrantRest());
        }
        return null;
    }
}
