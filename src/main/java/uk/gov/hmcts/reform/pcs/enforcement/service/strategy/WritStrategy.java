package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WritEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WritRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.WritDetailsMapper;

@Component
@AllArgsConstructor
public class WritStrategy implements EnforcementTypeStrategy {

    private final WritDetailsMapper writDetailsMapper;
    private final WritRepository writRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        WritDetails writDetails = enforcementOrder.getWritDetails();
        WritEntity writEntity = writDetailsMapper.toEntity(writDetails);
        writEntity.setEnforcementOrder(enforcementOrderEntity);
        writRepository.save(writEntity);
    }

}
