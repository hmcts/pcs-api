package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        // To be implemented
    }
}
