package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantDetailsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.RiskDetailsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.SelectedDefendantsMapper;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementWarrantRepository;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantStrategy implements EnforcementTypeStrategy {

    private final RiskProfileRepository riskProfileRepository;
    private final SelectedDefendantsMapper selectedDefendantsMapper;
    private final SelectedDefendantRepository selectedDefendantRepository;
    private final WarrantDetailsMapper warrantDetailsMapper;
    private final RiskDetailsMapper riskProfileMapper;
    private final EnforcementWarrantRepository enforcementWarrantRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        processWarrant(enforcementOrder, enforcementOrderEntity);
        processRisk(enforcementOrder, enforcementOrderEntity);
        processSelectedDefendants(enforcementOrderEntity);
    }

    private void processWarrant(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        EnforcementWarrantEntity warrantEntity = warrantDetailsMapper.toEntity(enforcementOrder,
                                                                                   enforcementOrderEntity);
        EnforcementWarrantEntity saved = enforcementWarrantRepository.save(warrantEntity);
        enforcementOrderEntity.setWarrantDetails(saved);
    }

    private void processRisk(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        RiskProfileEntity riskProfile = riskProfileMapper.toEntity(enforcementOrderEntity, enforcementOrder);
        riskProfileRepository.save(riskProfile);
    }

    private void processSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        List<SelectedDefendantEntity> selectedDefendantsEntities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);
        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            selectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
    }

}
