package uk.gov.hmcts.reform.pcs.enforcement.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableCategory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantRiskAnswersTest {

    @Test
    void shouldKeepTheDetailsOfTheChosenRiskCategoriesOnly() {
        ClaimantRiskAnswers answers = ClaimantRiskAnswers.builder()
            .anyRiskToBailiff(YesNoNotSure.YES)
            .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
            .violentDetails("Threw a chair")
            .animalsDetails("Left over from an earlier answer")
            .build();

        ClaimantRiskAnswers normalised = answers.normalised();

        assertThat(normalised.getRiskCategories()).containsExactly(RiskCategory.VIOLENT_OR_AGGRESSIVE);
        assertThat(normalised.getViolentDetails()).isEqualTo("Threw a chair");
        assertThat(normalised.getAnimalsDetails()).isNull();
    }

    @Test
    void shouldDropEveryRiskDetailWhenNobodyPosesARisk() {
        ClaimantRiskAnswers answers = ClaimantRiskAnswers.builder()
            .anyRiskToBailiff(YesNoNotSure.NOT_SURE)
            .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
            .violentDetails("Threw a chair")
            .build();

        ClaimantRiskAnswers normalised = answers.normalised();

        assertThat(normalised.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NOT_SURE);
        assertThat(normalised.getRiskCategories()).isNull();
        assertThat(normalised.getViolentDetails()).isNull();
    }

    @Test
    void shouldDropFollowUpAnswersToQuestionsAnsweredNo() {
        ClaimantRiskAnswers answers = ClaimantRiskAnswers.builder()
            .vulnerablePeoplePresent(YesNoNotSure.NO)
            .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
            .vulnerableReasonText("Old answer")
            .isDifficultToAccessProperty(VerticalYesNo.NO)
            .clarificationOnAccessDifficultyText("Old answer")
            .additionalInformationSelect(VerticalYesNo.NO)
            .additionalInformationDetails("Old answer")
            .build();

        assertThat(answers.normalised()).isEqualTo(ClaimantRiskAnswers.builder()
            .vulnerablePeoplePresent(YesNoNotSure.NO)
            .isDifficultToAccessProperty(VerticalYesNo.NO)
            .additionalInformationSelect(VerticalYesNo.NO)
            .build());
    }

    @Test
    void shouldTreatBlankTextAsNoAnswer() {
        ClaimantRiskAnswers answers = ClaimantRiskAnswers.builder()
            .isDifficultToAccessProperty(VerticalYesNo.YES)
            .clarificationOnAccessDifficultyText("  ")
            .build();

        assertThat(answers.normalised().getClarificationOnAccessDifficultyText()).isNull();
    }
}
