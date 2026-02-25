package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.RiskProfileEntity;

@Component
public class WarrantDetailsMapper {

    private final ModelMapper modelMapper;

    public WarrantDetailsMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public RiskProfileEntity toEntity(EnforcementOrderEntity enforcementOrderEntity,
                                      EnforcementOrder enforcementOrder) {
        RiskProfileEntity entity = new RiskProfileEntity();
        entity.setEnforcementOrder(enforcementOrderEntity);
        WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
        entity.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());
        EnforcementRiskDetails riskDetails = warrantDetails.getRiskDetails();
        if (riskDetails != null) {
            modelMapper.map(riskDetails, entity);
        }
        applyRawWarrantDetails(enforcementOrder, entity);
        return entity;
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
