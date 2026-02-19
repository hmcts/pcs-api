package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;

@Component
public class EnforcementRiskProfileMapper {

    private final ModelMapper modelMapper;

    public EnforcementRiskProfileMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public EnforcementRiskProfileEntity toEntity(
            EnforcementOrderEntity enforcementOrderEntity,
            EnforcementOrder enforcementOrder) {
        EnforcementRiskProfileEntity entity = new EnforcementRiskProfileEntity();
        entity.setEnforcementOrder(enforcementOrderEntity);

        WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
        if (warrantDetails != null) {
            entity.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());
            EnforcementRiskDetails riskDetails = warrantDetails.getRiskDetails();
            if (riskDetails != null) {
                modelMapper.map(riskDetails, entity);
            }
        }

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

        return entity;
    }
}
