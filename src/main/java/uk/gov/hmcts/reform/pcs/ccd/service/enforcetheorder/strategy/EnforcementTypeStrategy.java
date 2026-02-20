package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;

public interface EnforcementTypeStrategy {

    void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder);

}
