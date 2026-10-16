package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WritOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WritOfRestitutionRepository;

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
