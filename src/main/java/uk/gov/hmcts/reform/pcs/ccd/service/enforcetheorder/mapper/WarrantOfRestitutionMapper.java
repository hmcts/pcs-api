package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;

import java.util.HashSet;
import java.util.Set;

@Component
public class WarrantOfRestitutionMapper {

    public void prePopulateFieldsFromWarrantDetails(EnforcementOrder warrantEnforcementOrder,
                                                    EnforcementOrder currentEnfOrder) {
        WarrantDetails warrantDetails = warrantEnforcementOrder.getWarrantDetails();

        WarrantOfRestitutionDetails warrantRestDetails = new WarrantOfRestitutionDetails();
        warrantRestDetails.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());

        if (warrantDetails.getRiskCategories() != null) {
            Set<RiskCategory> riskCategorySet = warrantDetails.getRiskCategories();
            warrantRestDetails.setRiskCategories(new HashSet<>(riskCategorySet));
        }

        if (warrantDetails.getRiskDetails() != null) {
            RiskDetails src = warrantDetails.getRiskDetails();
            RiskDetails target = new RiskDetails();
            target.setViolentDetails(src.getViolentDetails());
            target.setFirearmsDetails(src.getFirearmsDetails());
            target.setCriminalDetails(src.getCriminalDetails());
            target.setVerbalThreatsDetails(src.getVerbalThreatsDetails());
            target.setProtestGroupDetails(src.getProtestGroupDetails());
            target.setPoliceSocialServicesDetails(src.getPoliceSocialServicesDetails());
            target.setAnimalsDetails(src.getAnimalsDetails());
            warrantRestDetails.setRiskDetails(target);
        }

        if (warrantDetails.getPropertyAccessDetails() != null) {
            PropertyAccessDetails src = warrantDetails.getPropertyAccessDetails();
            PropertyAccessDetails target = new PropertyAccessDetails();
            target.setIsDifficultToAccessProperty(src.getIsDifficultToAccessProperty());
            target.setClarificationOnAccessDifficultyText(src.getClarificationOnAccessDifficultyText());
            warrantRestDetails.setPropertyAccessDetails(target);
        }

        if (warrantDetails.getAdditionalInformation() != null) {
            AdditionalInformation additionalInformation = new AdditionalInformation();
            additionalInformation.setAdditionalInformationSelect(warrantDetails.getAdditionalInformation()
                    .getAdditionalInformationSelect());
            additionalInformation.setAdditionalInformationDetails(warrantDetails.getAdditionalInformation()
                    .getAdditionalInformationDetails());
            warrantRestDetails.setAdditionalInformation(additionalInformation);
        }
        currentEnfOrder.setWarrantOfRestitutionDetails(warrantRestDetails);

        RawWarrantDetails rawWarrantDetails = warrantEnforcementOrder.getRawWarrantDetails();
        RawWarrantRestDetails rawWarrantRestDetails = new RawWarrantRestDetails();

        rawWarrantRestDetails.setVulnerablePeoplePresentWarrantRest(rawWarrantDetails.getVulnerablePeoplePresent());
        if (rawWarrantDetails.getVulnerableAdultsChildren() != null) {
            VulnerableAdultsChildren src = rawWarrantDetails.getVulnerableAdultsChildren();
            VulnerableAdultsChildren target = new VulnerableAdultsChildren();
            target.setVulnerableCategory(src.getVulnerableCategory());
            target.setVulnerableReasonText(src.getVulnerableReasonText());
            rawWarrantRestDetails.setVulnerableAdultsChildrenWarrantRest(target);
        }

        currentEnfOrder.setRawWarrantRestDetails(rawWarrantRestDetails);
    }

    public WarrantOfRestitutionEntity toEntity(EnforcementOrder enforcementOrder,
                                               EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = WarrantOfRestitutionEntity.builder()
                .enforcementOrder(enforcementOrderEntity).build();
        if (enforcementOrder.getWarrantOfRestitutionDetails() != null) {
            WarrantOfRestitutionDetails warrantOfRestitutionDetails = enforcementOrder.getWarrantOfRestitutionDetails();
            mapDefendantReturned(warrantOfRestitutionEntity, warrantOfRestitutionDetails);
            mapAdditionalInformation(warrantOfRestitutionEntity, warrantOfRestitutionDetails);
            mapPropertyAccessDetails(warrantOfRestitutionEntity, warrantOfRestitutionDetails);
        }
        return warrantOfRestitutionEntity;
    }

    private void mapDefendantReturned(WarrantOfRestitutionEntity warrantOfRestitutionEntity,
                                      WarrantOfRestitutionDetails warrantOfRestitutionDetails) {
        warrantOfRestitutionEntity.setHowDefendantsReturned(warrantOfRestitutionDetails.getHowDefendantsReturned());
    }

    private void mapAdditionalInformation(WarrantOfRestitutionEntity warrantOfRestitutionEntity,
                                          WarrantOfRestitutionDetails warrantOfRestitutionDetails) {
        if (warrantOfRestitutionDetails.getAdditionalInformation() != null) {
            warrantOfRestitutionEntity.setAdditionalInformationSelect(
                    warrantOfRestitutionDetails.getAdditionalInformation().getAdditionalInformationSelect());
            warrantOfRestitutionEntity.setAdditionalInformationDetails(
                    warrantOfRestitutionDetails.getAdditionalInformation().getAdditionalInformationDetails());
        }
    }

    private void mapPropertyAccessDetails(WarrantOfRestitutionEntity warrantOfRestitutionEntity,
                                          WarrantOfRestitutionDetails warrantOfRestitutionDetails) {
        if (warrantOfRestitutionDetails.getPropertyAccessDetails() != null) {
            warrantOfRestitutionEntity.setIsDifficultToAccessProperty(
                    warrantOfRestitutionDetails.getPropertyAccessDetails().getIsDifficultToAccessProperty());
            warrantOfRestitutionEntity.setClarificationOnAccessDifficultyText(
                    warrantOfRestitutionDetails.getPropertyAccessDetails().getClarificationOnAccessDifficultyText());
        }
    }
}
