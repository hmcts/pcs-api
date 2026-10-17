package uk.gov.hmcts.reform.pcs.enforcement.service.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
public class WarrantOfRestitutionMapper {

    /**
     * Seeds the warrant of restitution's risk, vulnerability, property access and additional information
     * answers from the claimant's earlier warrant, so they do not have to give them again.
     *
     * <p>The "Your application" page runs this on every Continue, which includes continuing again after ExUI
     * "Previous" and after "Resume draft application", so it only seeds while the claimant has answered nothing
     * in the restitution journey. Once they have answered something, their answers stand: a claimant who
     * changed a seeded answer, or cleared it, keeps the change, and the explanation and evidence that only the
     * restitution journey collects are never dropped. Merging field by field would instead undo a cleared
     * answer, and could reinstate risk categories the claimant had abandoned by answering "no" to
     * {@code anyRiskToBailiff}.</p>
     */
    public void prePopulateFieldsFromWarrantDetails(EnforcementOrder warrantEnforcementOrder,
                                                    EnforcementOrder currentEnfOrder) {
        if (hasRestitutionAnswers(currentEnfOrder)) {
            return;
        }
        WarrantDetails warrantDetails = warrantEnforcementOrder.getWarrantDetails();

        WarrantOfRestitutionDetails warrantRestDetails = restitutionDetailsOf(currentEnfOrder);
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
        RawWarrantRestDetails rawWarrantRestDetails = rawRestitutionDetailsOf(currentEnfOrder);

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

    private static WarrantOfRestitutionDetails restitutionDetailsOf(EnforcementOrder enforcementOrder) {
        return enforcementOrder.getWarrantOfRestitutionDetails() != null
            ? enforcementOrder.getWarrantOfRestitutionDetails()
            : new WarrantOfRestitutionDetails();
    }

    private static RawWarrantRestDetails rawRestitutionDetailsOf(EnforcementOrder enforcementOrder) {
        return enforcementOrder.getRawWarrantRestDetails() != null
            ? enforcementOrder.getRawWarrantRestDetails()
            : new RawWarrantRestDetails();
    }

    /**
     * Whether the claimant has given any warrant of restitution answer.
     */
    private static boolean hasRestitutionAnswers(EnforcementOrder enforcementOrder) {
        return hasAnswers(enforcementOrder.getWarrantOfRestitutionDetails())
            || hasAnswers(enforcementOrder.getRawWarrantRestDetails());
    }

    private static boolean hasAnswers(WarrantOfRestitutionDetails details) {
        return details != null
            && (anyAnswered(details.getHowDefendantsReturned(), details.getAnyRiskToBailiff(),
                            details.getRiskCategories(), details.getAdditionalDocuments())
            || hasAnswers(details.getRiskDetails())
            || hasAnswers(details.getPropertyAccessDetails())
            || hasAnswers(details.getAdditionalInformation()));
    }

    private static boolean hasAnswers(RiskDetails riskDetails) {
        return riskDetails != null
            && anyAnswered(riskDetails.getViolentDetails(), riskDetails.getFirearmsDetails(),
                           riskDetails.getCriminalDetails(), riskDetails.getVerbalThreatsDetails(),
                           riskDetails.getProtestGroupDetails(), riskDetails.getPoliceSocialServicesDetails(),
                           riskDetails.getAnimalsDetails());
    }

    private static boolean hasAnswers(PropertyAccessDetails propertyAccessDetails) {
        return propertyAccessDetails != null
            && anyAnswered(propertyAccessDetails.getIsDifficultToAccessProperty(),
                           propertyAccessDetails.getClarificationOnAccessDifficultyText());
    }

    private static boolean hasAnswers(AdditionalInformation additionalInformation) {
        return additionalInformation != null
            && anyAnswered(additionalInformation.getAdditionalInformationSelect(),
                           additionalInformation.getAdditionalInformationDetails());
    }

    private static boolean hasAnswers(RawWarrantRestDetails details) {
        if (details == null) {
            return false;
        }
        VulnerableAdultsChildren vulnerable = details.getVulnerableAdultsChildrenWarrantRest();
        return anyAnswered(details.getVulnerablePeoplePresentWarrantRest())
            || vulnerable != null
            && anyAnswered(vulnerable.getVulnerableCategory(), vulnerable.getVulnerableReasonText());
    }

    private static boolean anyAnswered(Object... values) {
        return Arrays.stream(values).anyMatch(WarrantOfRestitutionMapper::isAnswered);
    }

    private static boolean isAnswered(Object value) {
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return value != null;
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
