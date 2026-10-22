package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

public interface EnforcementTypeStrategy {

    void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder);

}
