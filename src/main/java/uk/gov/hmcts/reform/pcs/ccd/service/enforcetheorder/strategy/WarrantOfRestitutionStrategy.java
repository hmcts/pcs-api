package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;

import java.time.Clock;
import java.time.LocalDate;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final Clock ukClock;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = new WarrantOfRestitutionEntity();
        warrantOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        warrantOfRestitutionEntity.setSubmissionDate(LocalDate.now(ukClock));
        warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
    }
}
