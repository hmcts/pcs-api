package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;

@Component
public class RiskDetailsMapper {

    private final ModelMapper modelMapper;

    public RiskDetailsMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public RiskProfileEntity toEntity(EnforcementOrderEntity enforcementOrderEntity,
                                      EnforcementOrder enforcementOrder) {
        RiskProfileEntity riskProfileEntity = new RiskProfileEntity();
        riskProfileEntity.setEnforcementOrder(enforcementOrderEntity);
        WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
        riskProfileEntity.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());
        RiskDetails riskDetails = warrantDetails.getRiskDetails();
        if (riskDetails != null) {
            modelMapper.map(riskDetails, riskProfileEntity);
        }
        applyRawWarrantDetails(enforcementOrder, riskProfileEntity);
        return riskProfileEntity;
    }

    private void applyRawWarrantDetails(EnforcementOrder enforcementOrder, RiskProfileEntity entity) {
        RawWarrantDetails rawWarrantDetails = enforcementOrder.getRawWarrantDetails();
        if (rawWarrantDetails != null) {
            entity.setVulnerablePeoplePresent(rawWarrantDetails.getVulnerablePeoplePresent());
            if (rawWarrantDetails.getVulnerableAdultsChildren() != null) {
                entity.setVulnerableCategory(
                    rawWarrantDetails.getVulnerableAdultsChildren().getVulnerableCategory());
                entity.setVulnerableReasonText(
                    rawWarrantDetails.getVulnerableAdultsChildren().getVulnerableReasonText());
            }
        }
    }
}
