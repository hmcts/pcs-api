package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatementOfTruthService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskProfileService;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.WarrantDetailsMapper;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.SelectedDefendantsMapper;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantStrategy implements EnforcementTypeStrategy {

    private final RiskProfileService riskProfileService;
    private final EnforcementStatementOfTruthService enforcementStatementOfTruthService;
    private final SelectedDefendantsMapper selectedDefendantsMapper;
    private final WarrantDetailsMapper warrantDetailsMapper;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        processWarrant(enforcementOrder, enforcementOrderEntity);
        riskProfileService.processRisk(enforcementOrder, enforcementOrderEntity);
        processStatementOfTruth(enforcementOrder, enforcementOrderEntity);
        processSelectedDefendants(enforcementOrderEntity);
    }

    private void processStatementOfTruth(EnforcementOrder enforcementOrder,
                                         EnforcementOrderEntity enforcementOrderEntity) {
        WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
        enforcementStatementOfTruthService.processStatementOfTruth(
                warrantDetails == null ? null : warrantDetails.getStatementOfTruth(), enforcementOrderEntity);
    }

    private void processWarrant(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        WarrantEntity warrantEntity = warrantDetailsMapper.toEntity(enforcementOrder,
                                                                    enforcementOrderEntity);
        enforcementOrderEntity.setWarrantDetails(warrantEntity);
    }

    private void processSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        List<SelectedDefendantEntity> selectedDefendantsEntities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);
        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            enforcementOrderEntity.addSelectedDefendants(selectedDefendantsEntities);
        }
    }

}
