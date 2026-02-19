package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.writ.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.WritRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.writ.WritDetailsMapper;

@Component
@AllArgsConstructor
public class WritStrategy implements EnforcementTypeStrategy {

    private final WritDetailsMapper writDetailsMapper;
    private final WritRepository writRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        WritDetails writDetails = enforcementOrder.getWritDetails();
        if (writDetails != null) {
            WritEntity writEntity = writDetailsMapper.toEntity(writDetails);
            writEntity.setEnforcementOrder(enforcementOrderEntity);
            writRepository.save(writEntity);
        }
    }

}
