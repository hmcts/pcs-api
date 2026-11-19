package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.RiskDetailsMapper;

@Service
@Slf4j
@AllArgsConstructor
public class RiskProfileService {

    private final RiskDetailsMapper riskProfileMapper;

    public void processRisk(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        RiskProfileEntity riskProfile = riskProfileMapper.toEntity(enforcementOrderEntity, enforcementOrder);
        enforcementOrderEntity.setRiskProfileDetails(riskProfile);
    }
}
