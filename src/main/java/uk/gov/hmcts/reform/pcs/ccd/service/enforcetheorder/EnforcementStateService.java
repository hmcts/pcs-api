package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementStateEntity;

@Service
@AllArgsConstructor
public class EnforcementStateService {

    public void mapToEntity(PcsCaseEntity pcsCaseEntity, EnforcementState enforcementState) {
        EnforcementStateEntity enforcementStateEntity =
                pcsCaseEntity.getEnforcementState() == null ? new EnforcementStateEntity()
                        : pcsCaseEntity.getEnforcementState();
        enforcementStateEntity.setPcsCase(pcsCaseEntity);
        enforcementStateEntity.setState(enforcementState);
        pcsCaseEntity.setEnforcementState(enforcementStateEntity);
    }
}
