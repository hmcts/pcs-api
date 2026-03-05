package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WritOfRestitutionRepository;

@Component
@AllArgsConstructor
public class WritOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final WritOfRestitutionRepository writOfRestitutionRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        WritOfRestitutionEntity writOfRestitutionEntity = new WritOfRestitutionEntity();
        writOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        writOfRestitutionRepository.save(writOfRestitutionEntity);
    }

}
