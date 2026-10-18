package uk.gov.hmcts.reform.pcs.enforcement.service.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Moves the claimant's risk assessment answers between {@link ClaimantRiskAnswers} and the fields of the
 * warrant application's risk pages, which "Confirm the eviction details" reuses.
 */
@Component
public class ClaimantRiskAnswersMapper {

    /**
     * The risk answers the claimant gave when they applied, or empty for a writ or an application that records
     * no enforcement type.
     */
    @SuppressWarnings("deprecation")
    public Optional<ClaimantRiskAnswers> fromApplication(EnforcementOrder application) {
        SelectEnforcementType type = SelectEnforcementType.from(application);
        if (type == SelectEnforcementType.WARRANT) {
            return Optional.of(fromWarrantFields(application));
        }
        if (type == SelectEnforcementType.WARRANT_OF_RESTITUTION) {
            return Optional.of(fromRestitutionFields(application));
        }
        return Optional.empty();
    }

    /**
     * The answers on the warrant risk pages, as "Confirm the eviction details" submits them.
     */
    @SuppressWarnings("deprecation")
    public ClaimantRiskAnswers fromWarrantFields(EnforcementOrder enforcementOrder) {
        ClaimantRiskAnswers.ClaimantRiskAnswersBuilder answers = ClaimantRiskAnswers.builder();
        WarrantDetails warrant = enforcementOrder.getWarrantDetails();
        if (warrant != null) {
            withRisks(answers, warrant.getAnyRiskToBailiff(), warrant.getRiskCategories(), warrant.getRiskDetails());
            withAccess(answers, warrant.getPropertyAccessDetails());
            withAdditionalInformation(answers, warrant.getAdditionalInformation());
        }
        RawWarrantDetails raw = enforcementOrder.getRawWarrantDetails();
        if (raw != null) {
            withVulnerability(answers, raw.getVulnerablePeoplePresent(), raw.getVulnerableAdultsChildren());
        }
        return answers.build().normalised();
    }

    /**
     * Puts the answers on the warrant risk pages and selects the warrant flow, so the reused pages show. Any other
     * warrant answers on the enforcement order are replaced: they belong to the application, not this journey.
     */
    @SuppressWarnings("deprecation")
    public void applyToWarrantFields(ClaimantRiskAnswers answers, EnforcementOrder enforcementOrder) {
        enforcementOrder.setChooseEnforcementType(warrantFlow());
        enforcementOrder.setWarrantDetails(WarrantDetails.builder()
            .anyRiskToBailiff(answers.getAnyRiskToBailiff())
            .riskCategories(answers.getRiskCategories() == null ? null : EnumSet.copyOf(answers.getRiskCategories()))
            .riskDetails(RiskDetails.builder()
                .violentDetails(answers.getViolentDetails())
                .firearmsDetails(answers.getFirearmsDetails())
                .criminalDetails(answers.getCriminalDetails())
                .verbalThreatsDetails(answers.getVerbalThreatsDetails())
                .protestGroupDetails(answers.getProtestGroupDetails())
                .policeSocialServicesDetails(answers.getPoliceSocialServicesDetails())
                .animalsDetails(answers.getAnimalsDetails())
                .build())
            .propertyAccessDetails(PropertyAccessDetails.builder()
                .isDifficultToAccessProperty(answers.getIsDifficultToAccessProperty())
                .clarificationOnAccessDifficultyText(answers.getClarificationOnAccessDifficultyText())
                .build())
            .additionalInformation(AdditionalInformation.builder()
                .additionalInformationSelect(answers.getAdditionalInformationSelect())
                .additionalInformationDetails(answers.getAdditionalInformationDetails())
                .build())
            .build());
        enforcementOrder.setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(answers.getVulnerablePeoplePresent())
            .vulnerableAdultsChildren(answers.getVulnerablePeoplePresent() == YesNoNotSure.YES
                ? VulnerableAdultsChildren.builder()
                    .vulnerableCategory(answers.getVulnerableCategory())
                    .vulnerableReasonText(answers.getVulnerableReasonText())
                    .build()
                : null)
            .build());
    }

    /**
     * Hides the warrant risk pages, for a claim with no warrant whose answers can be changed.
     */
    @SuppressWarnings("deprecation")
    public void clearWarrantFields(EnforcementOrder enforcementOrder) {
        enforcementOrder.setChooseEnforcementType(null);
        enforcementOrder.setWarrantDetails(null);
        enforcementOrder.setRawWarrantDetails(null);
    }

    private static ClaimantRiskAnswers fromRestitutionFields(EnforcementOrder application) {
        ClaimantRiskAnswers.ClaimantRiskAnswersBuilder answers = ClaimantRiskAnswers.builder();
        WarrantOfRestitutionDetails restitution = application.getWarrantOfRestitutionDetails();
        if (restitution != null) {
            withRisks(answers, restitution.getAnyRiskToBailiff(), restitution.getRiskCategories(),
                      restitution.getRiskDetails());
            withAccess(answers, restitution.getPropertyAccessDetails());
            withAdditionalInformation(answers, restitution.getAdditionalInformation());
        }
        RawWarrantRestDetails raw = application.getRawWarrantRestDetails();
        if (raw != null) {
            withVulnerability(answers, raw.getVulnerablePeoplePresentWarrantRest(),
                              raw.getVulnerableAdultsChildrenWarrantRest());
        }
        return answers.build().normalised();
    }

    private static void withRisks(ClaimantRiskAnswers.ClaimantRiskAnswersBuilder answers, YesNoNotSure anyRisk,
                                  Set<RiskCategory> categories, RiskDetails details) {
        answers.anyRiskToBailiff(anyRisk).riskCategories(categories);
        if (details != null) {
            answers.violentDetails(details.getViolentDetails())
                .firearmsDetails(details.getFirearmsDetails())
                .criminalDetails(details.getCriminalDetails())
                .verbalThreatsDetails(details.getVerbalThreatsDetails())
                .protestGroupDetails(details.getProtestGroupDetails())
                .policeSocialServicesDetails(details.getPoliceSocialServicesDetails())
                .animalsDetails(details.getAnimalsDetails());
        }
    }

    private static void withAccess(ClaimantRiskAnswers.ClaimantRiskAnswersBuilder answers,
                                   PropertyAccessDetails access) {
        if (access != null) {
            answers.isDifficultToAccessProperty(access.getIsDifficultToAccessProperty())
                .clarificationOnAccessDifficultyText(access.getClarificationOnAccessDifficultyText());
        }
    }

    private static void withAdditionalInformation(ClaimantRiskAnswers.ClaimantRiskAnswersBuilder answers,
                                                  AdditionalInformation additionalInformation) {
        if (additionalInformation != null) {
            VerticalYesNo select = additionalInformation.getAdditionalInformationSelect();
            answers.additionalInformationSelect(select)
                .additionalInformationDetails(additionalInformation.getAdditionalInformationDetails());
        }
    }

    private static void withVulnerability(ClaimantRiskAnswers.ClaimantRiskAnswersBuilder answers,
                                          YesNoNotSure present, VulnerableAdultsChildren vulnerable) {
        answers.vulnerablePeoplePresent(present);
        if (vulnerable != null) {
            answers.vulnerableCategory(vulnerable.getVulnerableCategory())
                .vulnerableReasonText(vulnerable.getVulnerableReasonText());
        }
    }

    private static DynamicStringList warrantFlow() {
        DynamicStringListElement warrant = new DynamicStringListElement(
            SelectEnforcementType.WARRANT.name(), SelectEnforcementType.WARRANT.getLabel());
        return new DynamicStringList(warrant, List.of(warrant));
    }
}
