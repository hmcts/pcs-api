package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.RiskDetailsMapper;

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
