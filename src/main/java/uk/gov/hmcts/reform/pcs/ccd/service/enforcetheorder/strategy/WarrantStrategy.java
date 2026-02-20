package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementRiskProfileMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.SelectedDefendantsMapper;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantStrategy implements EnforcementTypeStrategy {

    private final RiskProfileRepository riskProfileRepository;
    private final SelectedDefendantsMapper selectedDefendantsMapper;
    private final SelectedDefendantRepository selectedDefendantRepository;
    private final EnforcementRiskProfileMapper riskProfileMapper;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        RiskProfileEntity riskProfile = riskProfileMapper.toEntity(enforcementOrderEntity, enforcementOrder);
        riskProfileRepository.save(riskProfile);
        List<SelectedDefendantEntity> selectedDefendantsEntities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);
        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            selectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
    }

}
