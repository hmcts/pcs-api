package uk.gov.hmcts.reform.pcs.enforcement.service.mapper;

import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class ClaimantRiskAnswersMapperTest {

    private static final ClaimantRiskAnswers ANSWERS = ClaimantRiskAnswers.builder()
        .anyRiskToBailiff(YesNoNotSure.YES)
        .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.AGGRESSIVE_ANIMALS))
        .violentDetails("Threw a chair")
        .animalsDetails("Two dogs")
        .vulnerablePeoplePresent(YesNoNotSure.YES)
        .vulnerableCategory(VulnerableCategory.VULNERABLE_CHILDREN)
        .vulnerableReasonText("Young children")
        .isDifficultToAccessProperty(VerticalYesNo.YES)
        .clarificationOnAccessDifficultyText("Gated")
        .additionalInformationSelect(VerticalYesNo.YES)
        .additionalInformationDetails("Ring first")
        .build();

    private final ClaimantRiskAnswersMapper underTest = new ClaimantRiskAnswersMapper();

    @Test
    void shouldReadAWarrantApplicationsRiskAnswers() {
        EnforcementOrder application = warrantFieldsFor(SelectEnforcementType.WARRANT);

        assertThat(underTest.fromApplication(application)).contains(ANSWERS);
    }

    @Test
    void shouldReadAWarrantOfRestitutionApplicationsRiskAnswers() {
        EnforcementOrder application = EnforcementOrder.builder()
            .chooseEnforcementType(type(SelectEnforcementType.WARRANT_OF_RESTITUTION))
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                .anyRiskToBailiff(YesNoNotSure.YES)
                .riskCategories(ANSWERS.getRiskCategories())
                .riskDetails(RiskDetails.builder().violentDetails("Threw a chair").animalsDetails("Two dogs").build())
                .propertyAccessDetails(new PropertyAccessDetails(VerticalYesNo.YES, "Gated"))
                .additionalInformation(new AdditionalInformation(VerticalYesNo.YES, "Ring first"))
                .build())
            .rawWarrantRestDetails(RawWarrantRestDetails.builder()
                .vulnerablePeoplePresentWarrantRest(YesNoNotSure.YES)
                .vulnerableAdultsChildrenWarrantRest(
                    new VulnerableAdultsChildren(VulnerableCategory.VULNERABLE_CHILDREN, "Young children"))
                .build())
            .build();

        assertThat(underTest.fromApplication(application)).contains(ANSWERS);
    }

    @Test
    void shouldHaveNoRiskAnswersForAWrit() {
        EnforcementOrder application = EnforcementOrder.builder()
            .chooseEnforcementType(type(SelectEnforcementType.WRIT))
            .writDetails(WritDetails.builder().build())
            .build();

        assertThat(underTest.fromApplication(application)).isEmpty();
    }

    @Test
    void shouldPutTheAnswersOnTheWarrantPagesAndSelectTheWarrantFlow() {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(WarrantDetails.builder().defendantsDOBKnown(VerticalYesNo.YES).build())
            .build();

        underTest.applyToWarrantFields(ANSWERS, enforcementOrder);

        assertThat(enforcementOrder.getChooseEnforcementType().getValueCode()).isEqualTo("WARRANT");
        assertThat(enforcementOrder.getWarrantDetails().getDefendantsDOBKnown()).isNull();
        assertThat(underTest.fromWarrantFields(enforcementOrder)).isEqualTo(ANSWERS);
    }

    @Test
    void shouldHideTheWarrantPagesWhenThereAreNoAnswersToChange() {
        EnforcementOrder enforcementOrder = warrantFieldsFor(SelectEnforcementType.WARRANT);

        underTest.clearWarrantFields(enforcementOrder);

        assertThat(enforcementOrder.getChooseEnforcementType()).isNull();
        assertThat(enforcementOrder.getWarrantDetails()).isNull();
        assertThat(enforcementOrder.getRawWarrantDetails()).isNull();
    }

    @Test
    void shouldIgnoreAnswersToQuestionsTheClaimantWasNotAsked() {
        EnforcementOrder submitted = warrantFieldsFor(SelectEnforcementType.WARRANT);
        submitted.getWarrantDetails().setAnyRiskToBailiff(YesNoNotSure.NO);

        ClaimantRiskAnswers answers = underTest.fromWarrantFields(submitted);

        assertThat(answers.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
        assertThat(answers.getRiskCategories()).isNull();
        assertThat(answers.getViolentDetails()).isNull();
    }

    private static EnforcementOrder warrantFieldsFor(SelectEnforcementType type) {
        return EnforcementOrder.builder()
            .chooseEnforcementType(type(type))
            .warrantDetails(WarrantDetails.builder()
                .anyRiskToBailiff(YesNoNotSure.YES)
                .riskCategories(ANSWERS.getRiskCategories())
                .riskDetails(RiskDetails.builder().violentDetails("Threw a chair").animalsDetails("Two dogs").build())
                .propertyAccessDetails(new PropertyAccessDetails(VerticalYesNo.YES, "Gated"))
                .additionalInformation(new AdditionalInformation(VerticalYesNo.YES, "Ring first"))
                .build())
            .rawWarrantDetails(RawWarrantDetails.builder()
                .vulnerablePeoplePresent(YesNoNotSure.YES)
                .vulnerableAdultsChildren(
                    new VulnerableAdultsChildren(VulnerableCategory.VULNERABLE_CHILDREN, "Young children"))
                .build())
            .build();
    }

    private static DynamicStringList type(SelectEnforcementType type) {
        DynamicStringListElement element = new DynamicStringListElement(type.name(), type.getLabel());
        return new DynamicStringList(element, List.of(element));
    }
}
