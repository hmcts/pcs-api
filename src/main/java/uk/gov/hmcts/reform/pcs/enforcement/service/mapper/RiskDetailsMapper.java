package uk.gov.hmcts.reform.pcs.enforcement.service.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskProfileEntity;

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

        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcementOrder);
        if (enforcementType == SelectEnforcementType.WARRANT) {
            applyWarrantDetails(enforcementOrder.getWarrantDetails(), riskProfileEntity);
            applyRawWarrantDetails(enforcementOrder.getRawWarrantDetails(), riskProfileEntity);
        } else if (enforcementType == SelectEnforcementType.WARRANT_OF_RESTITUTION) {
            applyWarrantOfRestitutionDetails(enforcementOrder.getWarrantOfRestitutionDetails(), riskProfileEntity);
            applyRawWarrantRestDetails(enforcementOrder.getRawWarrantRestDetails(), riskProfileEntity);
        }
        return riskProfileEntity;
    }

    private void applyWarrantDetails(WarrantDetails warrantDetails, RiskProfileEntity riskProfileEntity) {
        riskProfileEntity.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());

        RiskDetails riskDetails = warrantDetails.getRiskDetails();
        if (riskDetails != null) {
            modelMapper.map(riskDetails, riskProfileEntity);
        }
    }

    private void applyRawWarrantDetails(RawWarrantDetails rawWarrantDetails, RiskProfileEntity entity) {
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

    private void applyWarrantOfRestitutionDetails(WarrantOfRestitutionDetails details,
                                                  RiskProfileEntity riskProfileEntity) {
        riskProfileEntity.setAnyRiskToBailiff(details.getAnyRiskToBailiff());

        RiskDetails riskDetails = details.getRiskDetails();
        if (riskDetails != null) {
            modelMapper.map(riskDetails, riskProfileEntity);
        }
    }

    private void applyRawWarrantRestDetails(RawWarrantRestDetails details, RiskProfileEntity entity) {
        if (details != null) {
            entity.setVulnerablePeoplePresent(details.getVulnerablePeoplePresentWarrantRest());
            if (details.getVulnerableAdultsChildrenWarrantRest() != null) {
                entity.setVulnerableCategory(
                        details.getVulnerableAdultsChildrenWarrantRest().getVulnerableCategory());
                entity.setVulnerableReasonText(
                        details.getVulnerableAdultsChildrenWarrantRest().getVulnerableReasonText());
            }
        }
    }
}
