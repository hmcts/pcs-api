package uk.gov.hmcts.reform.pcs.ccd.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.RiskAssessmentRenderer;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.PebbleConfiguration;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ASSESS_RISK_LEVEL;

class RiskAssessmentViewTest {

    private final FeatureToggleService featureToggleService = mock(FeatureToggleService.class);
    private final EnforcementOrderMediator enforcementOrderMediator =
        spy(new EnforcementOrderMediator(featureToggleService));
    private final RiskAssessmentView view = new RiskAssessmentView(
        new RiskAssessmentRenderer(new PebbleConfiguration().pebbleEngine()), enforcementOrderMediator,
        featureToggleService
    );
    private PCSCase pcsCase = PCSCase.builder().build();

    @BeforeEach
    void enableRiskAssessment() {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(true);
    }

    @Test
    void shouldSkipReadingAndRenderingWhenFlagIsOff() {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(false);
        EnforcementOrderMediator mediator = mock(EnforcementOrderMediator.class);
        RiskAssessmentRenderer renderer = mock(RiskAssessmentRenderer.class);
        final RiskAssessmentView disabledView = new RiskAssessmentView(renderer, mediator, featureToggleService);

        disabledView.setCaseFields(pcsCase,
            PcsCaseEntity.builder().enforcementOrders(Set.of(application(SelectEnforcementType.WARRANT))).build());

        assertNoAssessment();
        assertThat(pcsCase.getRiskAssessmentInternalMarkdownLabel()).isNull();
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdownLabel()).isNull();
        verifyNoInteractions(mediator, renderer);
    }

    @Test
    void shouldHideAssessmentOnTheNextCaseReadWhenFlagIsTurnedOff() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        PcsCaseEntity entity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder().build())).enforcementOrders(Set.of(application)).build();
        EnforcementEventShowConditionsView conditions = new EnforcementEventShowConditionsView(
            enforcementOrderMediator);
        conditions.setCaseFields(entity, pcsCase);
        view.setCaseFields(pcsCase, entity);
        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).isNotBlank();
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown()).isNotBlank();
        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.YES);

        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(false);
        pcsCase = PCSCase.builder().build();
        conditions.setCaseFields(entity, pcsCase);
        view.setCaseFields(pcsCase, entity);

        assertNoAssessment();
        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getEnfShowConditions().getRecordWarrantReturn()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldRenderSeparateInternalAndClaimantViewsWithSubmissionDate() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setPeopleToEvict(PeopleToEvict.builder().evictEveryone(VerticalYesNo.NO).build());
        warrant.setDefendantsDOBKnown(VerticalYesNo.YES);
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .selectedDefendants(DynamicMultiSelectStringList.builder()
                .value(List.of(option("selected", "Billy Wright"), option("second", "Jane Wright")))
                .listItems(List.of(option("unselected", "Unselected defendant")))
                .build())
            .build());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Bailiff note", "Risk assessment level", "Claimant risk assessment",
                      "not added yet", "not set yet", "select action",
                      "Information provided by the claimant on 10 October 2026",
                      "when they were applying for enforcement",
                      "Do you want to evict everyone at the property?", "Who do you want to evict?",
                      "Billy Wright", "Jane Wright", "Do you know the defendants’ dates of birth?")
            .doesNotContain("Unselected defendant");
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown())
            .contains("Your risk assessment", "Information provided by you on 10 October 2026",
                      "when you were applying for enforcement", "Billy Wright", "Jane Wright")
            .doesNotContain("Bailiff note", "Risk assessment level", "Risk level", "not added yet", "select action");
    }

    @ParameterizedTest
    @EnumSource(RiskCategory.class)
    void shouldDisplayOnlyTheSelectedRisk(RiskCategory category) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setAnyRiskToBailiff(YesNoNotSure.YES);
        warrant.setRiskCategories(Set.of(category));
        warrant.setRiskDetails(allRiskDetails());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .containsSubsequence(category.getLabel(), description(category));
        for (RiskCategory other : RiskCategory.values()) {
            if (other != category) {
                for (String html : List.of(pcsCase.getRiskAssessmentInternalMarkdown(),
                                           pcsCase.getRiskAssessmentClaimantMarkdown())) {
                    assertThat(html).doesNotContain(other.getLabel(), description(other));
                }
            }
        }
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown())
            .containsSubsequence(category.getLabel(), description(category));
    }

    @Test
    void shouldDisplayAllSevenSelectedRisks() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getEnforcementOrder().getWarrantDetails().setAnyRiskToBailiff(YesNoNotSure.YES);
        application.getEnforcementOrder().getWarrantDetails().setRiskCategories(Set.of(RiskCategory.values()));
        application.getEnforcementOrder().getWarrantDetails().setRiskDetails(allRiskDetails());

        render(application);

        for (RiskCategory category : RiskCategory.values()) {
            for (String html : List.of(pcsCase.getRiskAssessmentInternalMarkdown(),
                                       pcsCase.getRiskAssessmentClaimantMarkdown())) {
                assertThat(html).containsSubsequence(category.getLabel(), description(category));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = YesNoNotSure.class, names = {"NO", "NOT_SURE"})
    void shouldDisplayTheRecordedAnswerWithoutStaleRiskOrVulnerabilityDetails(YesNoNotSure answer) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setAnyRiskToBailiff(answer);
        warrant.setRiskCategories(Set.of(RiskCategory.values()));
        warrant.setRiskDetails(allRiskDetails());
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(answer)
            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS).vulnerableReasonText("Stale details").build())
            .build());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Does anyone living at the property pose a risk to the bailiff?",
                      "Is anyone living at the property vulnerable?", answer.getLabel())
            .doesNotContain("Stale details", VulnerableCategory.VULNERABLE_ADULTS.getLabel());
        for (RiskCategory category : RiskCategory.values()) {
            assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
                .doesNotContain(category.getLabel(), description(category));
        }
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldShowConditionalDetailsOnlyForYesAnswers(VerticalYesNo answer) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setDefendantsDOBKnown(answer);
        warrant.setDefendantsDOB(DefendantsDOB.builder().defendantsDOBDetails("Billy: 1 February 1978").build());
        warrant.setPropertyAccessDetails(PropertyAccessDetails.builder()
            .isDifficultToAccessProperty(answer).clarificationOnAccessDifficultyText("Communal entrance").build());
        warrant.setAdditionalInformation(AdditionalInformation.builder()
            .additionalInformationSelect(answer).additionalInformationDetails("Parking available").build());
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(answer == VerticalYesNo.YES ? YesNoNotSure.YES : YesNoNotSure.NO)
            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS_AND_CHILDREN)
                .vulnerableReasonText("Limited mobility").build())
            .build());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Is anyone living at the property vulnerable?", "Is it difficult to access the property?",
                      "Do you want to tell us anything else that could help with the eviction?", answer.getLabel());
        for (String html : List.of(pcsCase.getRiskAssessmentInternalMarkdown(),
                                   pcsCase.getRiskAssessmentClaimantMarkdown())) {
            if (answer == VerticalYesNo.YES) {
                assertThat(html).contains("Communal entrance", "Parking available", "Limited mobility",
                                          "Vulnerable adults and children", "Billy: 1 February 1978");
            } else {
                assertThat(html).doesNotContain("Communal entrance", "Parking available", "Limited mobility",
                                                "Vulnerable adults and children", "Billy: 1 February 1978");
            }
        }
    }

    @Test
    void shouldHideSkippedQuestionsAndTheirStaleDetails() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setPeopleToEvict(PeopleToEvict.builder().evictEveryone(VerticalYesNo.YES).build());
        warrant.setDefendantsDOB(DefendantsDOB.builder().defendantsDOBDetails("01/02/1978").build());
        warrant.setRiskDetails(RiskDetails.builder().violentDetails("Prototype risk description").build());
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .selectedDefendants(DynamicMultiSelectStringList.builder()
                .value(List.of(option("stale", "Previously selected defendant"))).build())
            .build());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Do you want to evict everyone at the property?")
            .doesNotContain("Who do you want to evict?", "Previously selected defendant", "Do you know the defendants",
                            "01/02/1978", "Prototype risk description", "Can you describe", "Not answered");
    }

    @Test
    void shouldRenderAnAssessmentWithNoOptionalAnswers() {
        render(application(SelectEnforcementType.WARRANT));

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Claimant risk assessment", "10 October 2026")
            .doesNotContain("Do you", "Does anyone", "Is anyone", "Not answered", "Print");
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WRIT", "WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldNotPopulateTheTabForOtherEnforcementTypes(SelectEnforcementType type) {
        render(application(type));

        assertNoAssessment();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementState.class,
        names = {"AWAITING_SUBMISSION_TO_HMCTS", "REQUESTED_FOR_DELETION", "DRAFT_DISCARDED"})
    void shouldNotPopulateTheTabForUnsubmittedApplications(EnforcementState state) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.setState(state);

        render(application);

        assertNoAssessment();
    }

    @Test
    void shouldNotPopulateAnAssessmentWithoutASubmissionDate() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getStatementOfTruth().setCompletedDate(null);
        render(application);
        assertNoAssessment();
    }

    @Test
    void shouldNotPopulateAnAssessmentWithoutAnAcceptedStatementOfTruth() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getStatementOfTruth().setAccepted(YesOrNo.NO);
        render(application);
        assertNoAssessment();
    }

    @Test
    void shouldNotPopulateAssessmentWhenThereAreNoApplications() {
        view.setCaseFields(pcsCase, PcsCaseEntity.builder().build());

        assertNoAssessment();
    }

    @Test
    void shouldUseTheActiveApplicationEvenWhenAnotherSubmissionOrDraftIsNewer() {
        EnforcementOrderEntity active = application(SelectEnforcementType.WARRANT);
        active.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 12, 12, 0));
        EnforcementOrderEntity newer = application(SelectEnforcementType.WARRANT);
        newer.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 13, 12, 0));
        EnforcementOrderEntity draft = application(SelectEnforcementType.WARRANT);
        draft.setState(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);
        draft.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 14, 12, 0));
        PcsCaseEntity entity = caseWithApplications(active, newer, draft);
        doReturn(Optional.of(active)).when(enforcementOrderMediator).getEnforcementOrder(entity);

        view.setCaseFields(pcsCase, entity);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("12 October 2026").doesNotContain("13 October 2026", "14 October 2026");
    }

    @Test
    void shouldNotFallBackToAnotherWarrantWhenTheActiveApplicationIsAWrit() {
        EnforcementOrderEntity warrant = application(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity active = application(SelectEnforcementType.WRIT);
        PcsCaseEntity entity = caseWithApplications(warrant, active);
        doReturn(Optional.of(active)).when(enforcementOrderMediator).getEnforcementOrder(entity);

        view.setCaseFields(pcsCase, entity);

        assertNoAssessment();
    }

    @Test
    void shouldUseTheSavedApplicationInsteadOfTheResumableDraft() {
        EnforcementOrderEntity submitted = application(SelectEnforcementType.WARRANT);
        submitted.getEnforcementOrder().getWarrantDetails().setAnyRiskToBailiff(YesNoNotSure.NO);
        pcsCase.setEnforcementOrder(EnforcementOrder.builder().warrantDetails(WarrantDetails.builder()
            .anyRiskToBailiff(YesNoNotSure.YES).riskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION))
            .build()).build());

        render(submitted);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains("No").doesNotContain("History of firearm");
    }

    @Test
    void shouldDisplayTheSubmissionDateInUkTime() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 6, 9, 23, 30));

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains("10 June 2026");
    }

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldUseTheSameActiveApplicationForVisibilityAndContent(SelectEnforcementType type) {
        EnforcementOrderEntity latest = application(type);
        latest.setState(EnforcementState.AWAITING_BAILIFF_ALLOCATION);
        latest.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 12, 12, 0));
        final EnforcementOrderEntity old = application(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity draft = application(SelectEnforcementType.WARRANT);
        draft.setState(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);
        draft.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 13, 12, 0));
        PcsCaseEntity entity = caseWithApplications(old, latest, draft);
        doReturn(Optional.of(latest)).when(enforcementOrderMediator).getEnforcementOrder(entity);

        new EnforcementEventShowConditionsView(enforcementOrderMediator).setCaseFields(entity, pcsCase);
        view.setCaseFields(pcsCase, entity);

        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment())
            .isEqualTo(type == SelectEnforcementType.WARRANT ? YesOrNo.YES : YesOrNo.NO);
        if (type == SelectEnforcementType.WARRANT) {
            assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains("12 October 2026");
        } else {
            assertNoAssessment();
        }
    }

    @ParameterizedTest
    @EnumSource(EnforcementState.class)
    void shouldRenderOnlyForEligibleStates(EnforcementState state) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.setState(state);
        boolean eligible = switch (state) {
            case AWAITING_SUBMISSION_TO_HMCTS, PENDING_CASE_ISSUED, REQUESTED_FOR_DELETION, DRAFT_DISCARDED -> false;
            default -> true;
        };
        RiskAssessmentRenderer renderer = mock(RiskAssessmentRenderer.class);
        RiskAssessmentView underTest = new RiskAssessmentView(renderer, enforcementOrderMediator, featureToggleService);

        underTest.setCaseFields(pcsCase, caseWithApplications(application));

        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.from(eligible));
        if (eligible) {
            verify(renderer).render("10 October 2026", List.of(), false);
            verify(renderer).render("10 October 2026", List.of(), true);
        } else {
            assertNoAssessment();
            verifyNoInteractions(renderer);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSerializeNestedConditionsAndPreserveWarrantReturnForBothFlagStates(boolean enabled) {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(enabled);
        PcsCaseEntity entity = caseWithApplications(application(SelectEnforcementType.WARRANT));
        new EnforcementEventShowConditionsView(enforcementOrderMediator).setCaseFields(entity, pcsCase);

        view.setCaseFields(pcsCase, entity);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = mapper.valueToTree(pcsCase);
        JsonNode conditions = data.path("enfShowConditions");
        assertThat(conditions.path("recordWarrantReturn")).isEqualTo(mapper.valueToTree(YesOrNo.YES));
        assertThat(conditions.path("canViewRiskAssessment")).isEqualTo(mapper.valueToTree(YesOrNo.from(enabled)));
        assertThat(conditions.has("RecordWarrantReturn")).isFalse();
        assertThat(conditions.has("CanViewRiskAssessment")).isFalse();
        assertThat(data.has("enforcementEventShowConditions")).isFalse();
        assertThat(data.has("enfShowConditions_RecordWarrantReturn")).isFalse();
        assertThat(data.has("enfShowConditions_CanViewRiskAssessment")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(VulnerableCategory.class)
    void shouldShowWhoIsVulnerableBeforeTheirDetails(VulnerableCategory category) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(YesNoNotSure.YES)
            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                .vulnerableCategory(category).vulnerableReasonText("Support required").build()).build());

        render(application);

        for (String html : List.of(pcsCase.getRiskAssessmentInternalMarkdown(),
                                   pcsCase.getRiskAssessmentClaimantMarkdown())) {
            assertThat(html).containsSubsequence(category.getLabel(), "Support required");
        }
    }

    @Test
    void shouldEscapeRiskDescriptionsAndOmitBlankDetails() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setAnyRiskToBailiff(YesNoNotSure.YES);
        warrant.setRiskCategories(Set.of(RiskCategory.values()));
        warrant.setRiskDetails(RiskDetails.builder().violentDetails("<script>unsafe</script>")
            .firearmsDetails("  ").build());

        render(application);

        for (String html : List.of(pcsCase.getRiskAssessmentInternalMarkdown(),
                                   pcsCase.getRiskAssessmentClaimantMarkdown())) {
            assertThat(html).contains("&#60;script&#62;unsafe&#60;/script&#62;")
                .doesNotContain("<script>", "null");
        }
    }

    private RiskDetails allRiskDetails() {
        return RiskDetails.builder()
            .violentDetails(description(RiskCategory.VIOLENT_OR_AGGRESSIVE))
            .firearmsDetails(description(RiskCategory.FIREARMS_POSSESSION))
            .criminalDetails(description(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .verbalThreatsDetails(description(RiskCategory.VERBAL_OR_WRITTEN_THREATS))
            .protestGroupDetails(description(RiskCategory.PROTEST_GROUP_MEMBER))
            .policeSocialServicesDetails(description(RiskCategory.AGENCY_VISITS))
            .animalsDetails(description(RiskCategory.AGGRESSIVE_ANIMALS)).build();
    }

    private String description(RiskCategory category) {
        return "Description for " + category.name().replace('_', ' ');
    }

    private void assertNoAssessment() {
        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).isNull();
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown()).isNull();
        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
    }

    private void render(EnforcementOrderEntity application) {
        view.setCaseFields(pcsCase, caseWithApplications(application));
    }

    private PcsCaseEntity caseWithApplications(EnforcementOrderEntity... applications) {
        return PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder().build()))
            .enforcementOrders(Set.of(applications))
            .build();
    }

    private EnforcementOrderEntity application(SelectEnforcementType type) {
        return EnforcementOrderEntity.builder()
            .state(EnforcementState.CASE_PROGRESSION)
            .statementOfTruth(StatementOfTruthEntity.builder()
                .accepted(YesOrNo.YES).completedDate(LocalDateTime.of(2026, 10, 10, 12, 0)).build())
            .enforcementOrder(EnforcementOrder.builder()
                .chooseEnforcementType(DynamicStringList.builder().value(option(type.name(), type.getLabel())).build())
                .warrantDetails(WarrantDetails.builder().build())
                .build())
            .build();
    }

    private DynamicStringListElement option(String code, String label) {
        return DynamicStringListElement.builder().code(code).label(label).build();
    }
}
