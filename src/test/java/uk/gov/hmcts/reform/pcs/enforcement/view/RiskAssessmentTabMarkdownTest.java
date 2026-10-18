package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class RiskAssessmentTabMarkdownTest {

    private static final List<String> FEATURE_QUESTIONS = List.of(
        "Do you want to evict everyone at the property?",
        "Who do you want to evict?",
        "Do you know the defendants&#39; dates of birth?",
        "Does anyone at the property pose a risk to the bailiff?",
        "What kind of risks do they pose to the bailiff?",
        "How have they been violent or aggressive?",
        "What is their history of criminal or antisocial behaviour?",
        "What kind of animal do they have?",
        "Is anyone living at the property vulnerable?",
        "Is it difficult to access the property?",
        "Do you want to tell us anything else that could help with the eviction?"
    );

    @Test
    void shouldOpenTheCaseOnTheTabInANewBrowserTab() {
        assertThat(RiskAssessmentTabMarkdown.printButton("PCS", "PCS-123", 1234567890123456L)).isEqualTo(
            "<a class=\"govuk-button\" href=\"/cases/case-details/PCS/PCS-123/1234567890123456#Risk%20assessment\" "
                + "target=\"_blank\" rel=\"noopener noreferrer\">Print</a>");
    }

    @Test
    void shouldPromptForANoteAndARiskLevelBeforeEitherIsAdded() {
        String markdown = RiskAssessmentTabMarkdown.internalSections(List.of(), null, null);

        assertThat(markdown)
            .contains("<h2 class=\"govuk-heading-l\">Bailiff note</h2>")
            .contains("<p class=\"govuk-body\">Not added yet (click on the &#39;select action&#39; button above)</p>")
            .contains("<h2 class=\"govuk-heading-l\">Risk assessment level</h2>")
            .contains("<p class=\"govuk-body\">Not set yet (click on the &#39;select action&#39; button above)</p>");
        assertThat(markdown.indexOf("Bailiff note")).isLessThan(markdown.indexOf("Risk assessment level"));
    }

    @Test
    void shouldShowEachNoteWithItsAuthorAndTimeAndTheRiskLevelWithItsReason() {
        BailiffNoteEntity note = BailiffNoteEntity.builder()
            .note("Rear gate is locked")
            .authorFirstName("Sam")
            .authorLastName("Jones")
            .createdAt(Instant.parse("2026-02-01T16:20:00Z"))
            .build();

        String markdown = RiskAssessmentTabMarkdown.internalSections(List.of(note), RiskLevel.HIGH, "Known violence");

        assertThat(markdown)
            .contains(row("Note", "Rear gate is locked"))
            .contains(row("Added by", "Sam Jones"))
            .contains(row("Date and time", "1 Feb 2026 16:20pm"))
            .contains(row("Risk level", "High"))
            .contains(row("Why the risk level is high", "Known violence"))
            .doesNotContain("Not added yet", "Not set yet");
    }

    @Test
    void shouldNotShowAReasonForAMediumRiskLevel() {
        String markdown = RiskAssessmentTabMarkdown.internalSections(List.of(), RiskLevel.MEDIUM, "old reason");

        assertThat(markdown).contains(row("Risk level", "Medium")).doesNotContain("Why the risk level is high");
    }

    @Test
    void shouldIntroduceTheAnswersAsTheClaimantsForInternalUsers() {
        String markdown = RiskAssessmentTabMarkdown.claimantSection(
            warrantAnswers(), Instant.parse("2026-10-10T09:00:00Z"), false);

        assertThat(markdown)
            .contains("<h2 class=\"govuk-heading-l\">Claimant risk level</h2>")
            .contains("Information provided by the claimant on 10 October 2026 when they were applying for "
                          + "enforcement");
    }

    @Test
    void shouldIntroduceTheAnswersAsTheirOwnForTheClaimant() {
        String markdown = RiskAssessmentTabMarkdown.claimantSection(
            warrantAnswers(), Instant.parse("2026-10-10T09:00:00Z"), true);

        assertThat(markdown)
            .contains("Information you provided on 10 October 2026 when you were applying for enforcement")
            .doesNotContain("the claimant");
    }

    @Test
    void shouldLeaveTheDateOutWhenTheApplicationPredatesRecordingIt() {
        String markdown = RiskAssessmentTabMarkdown.claimantSection(warrantAnswers(), null, false);

        assertThat(markdown).contains(
            "<p class=\"govuk-body\">Information provided by the claimant when they were applying for enforcement</p>");
    }

    @Test
    void shouldAskEveryQuestionTheFeatureListsInOrderEvenWhenUnanswered() {
        String markdown = RiskAssessmentTabMarkdown.claimantSection(
            RiskAssessmentTabMarkdown.answersOf(new EnforcementOrder(), false), null, false);

        int previous = -1;
        for (String question : FEATURE_QUESTIONS) {
            int index = markdown.indexOf("<dt class=\"govuk-summary-list__key\">" + question + "</dt>");
            assertThat(index).as(question).isGreaterThan(previous);
            previous = index;
        }
        assertThat(markdown).contains(row("Who do you want to evict?", "Not answered"));
    }

    @Test
    void shouldShowTheClaimantsWarrantAnswers() {
        String markdown = RiskAssessmentTabMarkdown.claimantSection(warrantAnswers(), null, false);

        assertThat(markdown)
            .contains(row("Do you want to evict everyone at the property?", "No"))
            .contains(row("Who do you want to evict?", "TVR HN<br>Test1 Testing1"))
            .contains(row("Do you know the defendants&#39; dates of birth?", "Yes"))
            .contains(row("What are the defendants&#39; dates of birth?", "TVR HN - 16 4 1991"))
            .contains(row("Does anyone at the property pose a risk to the bailiff?", "Yes"))
            .contains(row("What kind of risks do they pose to the bailiff?",
                          "Violent or aggressive behaviour<br>History of firearm possession"))
            .contains(row("How have they been violent or aggressive?", "Threw a chair"))
            .contains(row("What is their history of firearm possession?", "Shotgun licence"))
            .contains(row("What is their history of criminal or antisocial behaviour?", "Not answered"))
            .contains(row("Is anyone living at the property vulnerable?", "Yes"))
            .contains(row("How are they vulnerable?", "Elderly parent"))
            .contains(row("Is it difficult to access the property?", "Yes"))
            .contains(row("Explain why it&#39;s difficult to access the property", "Gated"))
            .contains(row("Do you want to tell us anything else that could help with the eviction?", "No"))
            .doesNotContain("What kind of verbal or written threats have they made?");
    }

    @Test
    void shouldShowTheWarrantOfRestitutionAnswersWithoutTheQuestionsItDoesNotAsk() {
        EnforcementOrder order = EnforcementOrder.builder()
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                                             .anyRiskToBailiff(YesNoNotSure.NOT_SURE)
                                             .build())
            .build();

        String markdown = RiskAssessmentTabMarkdown.claimantSection(
            RiskAssessmentTabMarkdown.answersOf(order, true), null, false);

        assertThat(markdown)
            .contains(row("Does anyone at the property pose a risk to the bailiff?", "I&rsquo;m not sure"))
            .doesNotContain("Do you want to evict everyone at the property?", "Who do you want to evict?",
                            "dates of birth");
    }

    @Test
    void shouldShowOriginalAndUpdatedAnswersSideBySideWithUnchangedAnswersLeftBlank() {
        RiskAssessmentTabMarkdown.ClaimantAnswers original = warrantAnswers();
        RiskAssessmentTabMarkdown.ClaimantAnswers updated = RiskAssessmentTabMarkdown.withRiskAnswers(
            original, ClaimantRiskAnswers.builder()
                .anyRiskToBailiff(YesNoNotSure.YES)
                .riskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION, RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .violentDetails("Threw a chair")
                .firearmsDetails("Shotgun licence")
                .vulnerablePeoplePresent(YesNoNotSure.NO)
                .isDifficultToAccessProperty(VerticalYesNo.YES)
                .clarificationOnAccessDifficultyText("Gated")
                .additionalInformationSelect(VerticalYesNo.NO)
                .build());

        String markdown = RiskAssessmentTabMarkdown.claimantSection(
            original, Instant.parse("2026-02-25T10:00:00Z"), updated, Instant.parse("2026-03-06T10:00:00Z"), false);

        assertThat(markdown)
            .contains("Information provided by the claimant on 25 February 2026")
            .contains("The claimant updated this information on 6 March 2026 when they confirmed the eviction")
            .contains("<th scope=\"row\" class=\"govuk-table__header\">"
                          + "Is anyone living at the property vulnerable?</th>"
                          + "<td class=\"govuk-table__cell\">Yes</td><td class=\"govuk-table__cell\">No</td>")
            .contains("<th scope=\"row\" class=\"govuk-table__header\">How are they vulnerable?</th>"
                          + "<td class=\"govuk-table__cell\">Elderly parent</td>"
                          + "<td class=\"govuk-table__cell\">Not answered</td>")
            .contains("<th scope=\"row\" class=\"govuk-table__header\">Is it difficult to access the property?</th>"
                          + "<td class=\"govuk-table__cell\">Yes</td><td class=\"govuk-table__cell\"></td>")
            .contains("<th scope=\"row\" class=\"govuk-table__header\">Who do you want to evict?</th>")
            .doesNotContain("\n\n");
    }

    @Test
    void shouldListOnlyTheChangedQuestionsInTheBanner() {
        RiskAssessmentTabMarkdown.ClaimantAnswers original = warrantAnswers();
        RiskAssessmentTabMarkdown.ClaimantAnswers updated = RiskAssessmentTabMarkdown.withRiskAnswers(
            original, ClaimantRiskAnswers.builder()
                .anyRiskToBailiff(YesNoNotSure.NO)
                .vulnerablePeoplePresent(YesNoNotSure.YES)
                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                .vulnerableReasonText("Elderly parent")
                .isDifficultToAccessProperty(VerticalYesNo.YES)
                .clarificationOnAccessDifficultyText("Gated")
                .additionalInformationSelect(VerticalYesNo.NO)
                .build());

        String banner = RiskAssessmentTabMarkdown.updateBanner(original, updated);

        assertThat(banner)
            .contains(RiskAssessmentTabMarkdown.UPDATE_BANNER)
            .contains("<li>Does anyone at the property pose a risk to the bailiff?</li>")
            .contains("<li>What kind of risks do they pose to the bailiff?</li>")
            .doesNotContain("<li>Is anyone living at the property vulnerable?</li>")
            .doesNotContain("<li>Who do you want to evict?</li>")
            .doesNotContain("\n\n");
    }

    private static RiskAssessmentTabMarkdown.ClaimantAnswers warrantAnswers() {
        EnforcementOrder order = EnforcementOrder.builder()
            .warrantDetails(WarrantDetails.builder()
                                .peopleToEvict(PeopleToEvict.builder().evictEveryone(VerticalYesNo.NO).build())
                                .defendantsDOBKnown(VerticalYesNo.YES)
                                .defendantsDOB(uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.DefendantsDOB
                                                   .builder().defendantsDOBDetails("TVR HN - 16 4 1991").build())
                                .anyRiskToBailiff(YesNoNotSure.YES)
                                .riskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION,
                                                       RiskCategory.VIOLENT_OR_AGGRESSIVE))
                                .riskDetails(RiskDetails.builder()
                                                 .violentDetails("Threw a chair")
                                                 .firearmsDetails("Shotgun licence")
                                                 .build())
                                .propertyAccessDetails(PropertyAccessDetails.builder()
                                                           .isDifficultToAccessProperty(VerticalYesNo.YES)
                                                           .clarificationOnAccessDifficultyText("Gated")
                                                           .build())
                                .additionalInformation(AdditionalInformation.builder()
                                                           .additionalInformationSelect(VerticalYesNo.NO)
                                                           .build())
                                .build())
            .rawWarrantDetails(RawWarrantDetails.builder()
                                   .vulnerablePeoplePresent(YesNoNotSure.YES)
                                   .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                                       .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                                       .vulnerableReasonText("Elderly parent")
                                       .build())
                                   .selectedDefendants(new DynamicMultiSelectStringList(
                                       List.of(element("1", "TVR HN"), element("2", "Test1 Testing1")), List.of()))
                                   .build())
            .build();
        return RiskAssessmentTabMarkdown.answersOf(order, false);
    }

    private static DynamicStringListElement element(String code, String label) {
        return DynamicStringListElement.builder().code(code).label(label).build();
    }

    private static String row(String key, String value) {
        return "<dt class=\"govuk-summary-list__key\">" + key + "</dt>"
            + "<dd class=\"govuk-summary-list__value\">" + value + "</dd>";
    }
}
