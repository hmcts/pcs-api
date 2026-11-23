package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ASSESS_RISK_LEVEL;

class RiskAssessmentViewTest {

    private final FeatureToggleService featureToggleService = mock(FeatureToggleService.class);
    private final RiskAssessmentView view = new RiskAssessmentView(
        new RiskAssessmentRenderer(new PebbleConfiguration().pebbleEngine()), new EnforcementOrderMediator(),
        featureToggleService
    );
    private final PCSCase pcsCase = PCSCase.builder().build();

    @BeforeEach
    void enableRiskAssessment() {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(true);
    }

    @Test
    void shouldClearRiskAssessmentWithoutReadingOrRenderingWhenFlagIsOff() {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(false);
        EnforcementOrderMediator mediator = mock(EnforcementOrderMediator.class);
        RiskAssessmentRenderer renderer = mock(RiskAssessmentRenderer.class);
        final RiskAssessmentView disabledView = new RiskAssessmentView(renderer, mediator, featureToggleService);
        pcsCase.setRiskAssessmentInternalMarkdown("Previous internal assessment");
        pcsCase.setRiskAssessmentClaimantMarkdown("Previous claimant assessment");
        pcsCase.setRiskAssessmentInternalMarkdownLabel("Previous internal label");
        pcsCase.setRiskAssessmentClaimantMarkdownLabel("Previous claimant label");

        disabledView.setCaseFields(pcsCase,
            PcsCaseEntity.builder().enforcementOrders(Set.of(application(SelectEnforcementType.WARRANT))).build());

        assertNoAssessment();
        assertThat(pcsCase.getRiskAssessmentInternalMarkdownLabel()).isNull();
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdownLabel()).isNull();
        verifyNoInteractions(mediator, renderer);
    }

    @Test
    void shouldHideAndClearAnExistingAssessmentWhenFlagIsTurnedOff() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        PcsCaseEntity entity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder().build())).enforcementOrders(Set.of(application)).build();
        EnforcementEventShowConditionsView conditions = new EnforcementEventShowConditionsView(
            new EnforcementOrderMediator(), featureToggleService);
        view.setCaseFields(pcsCase, entity);
        conditions.setCaseFields(entity, pcsCase);
        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).isNotBlank();
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown()).isNotBlank();
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.YES);

        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(false);
        view.setCaseFields(pcsCase, entity);
        conditions.setCaseFields(entity, pcsCase);

        assertNoAssessment();
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanRecordWarrantReturn()).isEqualTo(YesOrNo.YES);
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
                      "Billy Wright", "Jane Wright", "Do you know the defendants")
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

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains(category.getLabel());
        for (RiskCategory other : RiskCategory.values()) {
            if (other != category) {
                assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).doesNotContain(other.getLabel());
            }
        }
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown()).contains(category.getLabel());
    }

    @Test
    void shouldDisplayAllSevenSelectedRisks() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getEnforcementOrder().getWarrantDetails().setAnyRiskToBailiff(YesNoNotSure.YES);
        application.getEnforcementOrder().getWarrantDetails().setRiskCategories(Set.of(RiskCategory.values()));

        render(application);

        for (RiskCategory category : RiskCategory.values()) {
            assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains(category.getLabel());
        }
    }

    @ParameterizedTest
    @EnumSource(value = YesNoNotSure.class, names = {"NO", "NOT_SURE"})
    void shouldDisplayTheRecordedAnswerWithoutStaleRiskOrVulnerabilityDetails(YesNoNotSure answer) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setAnyRiskToBailiff(answer);
        warrant.setRiskCategories(Set.of(RiskCategory.values()));
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(answer)
            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder().vulnerableReasonText("Stale details").build())
            .build());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Does anyone living at the property pose a risk to the bailiff?",
                      "Is anyone living at the property vulnerable?", answer.getLabel())
            .doesNotContain("Stale details");
        for (RiskCategory category : RiskCategory.values()) {
            assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).doesNotContain(category.getLabel());
        }
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldShowConditionalDetailsOnlyForYesAnswers(VerticalYesNo answer) {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        WarrantDetails warrant = application.getEnforcementOrder().getWarrantDetails();
        warrant.setPropertyAccessDetails(PropertyAccessDetails.builder()
            .isDifficultToAccessProperty(answer).clarificationOnAccessDifficultyText("Communal entrance").build());
        warrant.setAdditionalInformation(AdditionalInformation.builder()
            .additionalInformationSelect(answer).additionalInformationDetails("Parking available").build());
        application.getEnforcementOrder().setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(answer == VerticalYesNo.YES ? YesNoNotSure.YES : YesNoNotSure.NO)
            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                .vulnerableReasonText("Limited mobility").build())
            .build());

        render(application);

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown())
            .contains("Is anyone living at the property vulnerable?", "Is it difficult to access the property?",
                      "Do you want to tell us anything else that could help with the eviction?", answer.getLabel());
        for (String html : List.of(pcsCase.getRiskAssessmentInternalMarkdown(),
                                   pcsCase.getRiskAssessmentClaimantMarkdown())) {
            if (answer == VerticalYesNo.YES) {
                assertThat(html).contains("Communal entrance", "Parking available", "Limited mobility");
            } else {
                assertThat(html).doesNotContain("Communal entrance", "Parking available", "Limited mobility");
            }
        }
    }

    @Test
    void shouldHideSkippedQuestionsAndNotUseTheOutdatedPrototypeFields() {
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
    void shouldClearPreviouslyPopulatedFieldsWhenThereAreNoApplications() {
        render(application(SelectEnforcementType.WARRANT));

        view.setCaseFields(pcsCase, PcsCaseEntity.builder().build());

        assertNoAssessment();
    }

    @Test
    void shouldUseTheLatestSubmittedApplicationAndIgnoreANewerDraft() {
        final EnforcementOrderEntity old = application(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity latest = application(SelectEnforcementType.WARRANT);
        latest.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 12, 12, 0));
        EnforcementOrderEntity draft = application(SelectEnforcementType.WARRANT);
        draft.setState(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);
        draft.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 13, 12, 0));

        view.setCaseFields(pcsCase, PcsCaseEntity.builder().enforcementOrders(Set.of(old, latest, draft)).build());

        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains("12 October 2026");
    }

    @Test
    void shouldNotFallBackToAnOldWarrantWhenTheLatestApplicationIsAWrit() {
        final EnforcementOrderEntity old = application(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity latest = application(SelectEnforcementType.WRIT);
        latest.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 12, 12, 0));

        view.setCaseFields(pcsCase, PcsCaseEntity.builder().enforcementOrders(Set.of(old, latest)).build());

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
    void shouldUseTheSameSubmittedApplicationForVisibilityAndContent(SelectEnforcementType type) {
        EnforcementOrderEntity latest = application(type);
        latest.setState(EnforcementState.AWAITING_BAILIFF_ALLOCATION);
        latest.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 12, 12, 0));
        final EnforcementOrderEntity old = application(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity draft = application(SelectEnforcementType.WARRANT);
        draft.setState(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);
        draft.getStatementOfTruth().setCompletedDate(LocalDateTime.of(2026, 10, 13, 12, 0));
        PcsCaseEntity entity = PcsCaseEntity.builder().enforcementOrders(Set.of(old, latest, draft)).build();

        view.setCaseFields(pcsCase, entity);
        new EnforcementEventShowConditionsView(new EnforcementOrderMediator(), featureToggleService)
            .setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnforcementEventShowConditions().getCanViewRiskAssessment())
            .isEqualTo(type == SelectEnforcementType.WARRANT ? YesOrNo.YES : YesOrNo.NO);
        if (type == SelectEnforcementType.WARRANT) {
            assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).contains("12 October 2026");
        } else {
            assertNoAssessment();
        }
    }

    @Test
    void shouldHideTheTabForAnApplicationAwaitingIssueAndResetVisibilityAfterEligibleStates() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        PcsCaseEntity entity = PcsCaseEntity.builder().enforcementOrders(Set.of(application)).build();
        EnforcementEventShowConditionsView conditions = new EnforcementEventShowConditionsView(
            new EnforcementOrderMediator(), featureToggleService);
        conditions.setCaseFields(entity, pcsCase);
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.YES);

        application.setState(EnforcementState.PENDING_CASE_ISSUED);
        conditions.setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnforcementEventShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldHideTheTabWhenThereIsNoCompletedSubmission() {
        EnforcementOrderEntity application = application(SelectEnforcementType.WARRANT);
        application.getStatementOfTruth().setCompletedDate(null);
        PcsCaseEntity entity = PcsCaseEntity.builder().enforcementOrders(Set.of(application)).build();

        new EnforcementEventShowConditionsView(new EnforcementOrderMediator(), featureToggleService)
            .setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnforcementEventShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
    }

    private void assertNoAssessment() {
        assertThat(pcsCase.getRiskAssessmentInternalMarkdown()).isNull();
        assertThat(pcsCase.getRiskAssessmentClaimantMarkdown()).isNull();
    }

    private void render(EnforcementOrderEntity application) {
        view.setCaseFields(pcsCase, PcsCaseEntity.builder().enforcementOrders(Set.of(application)).build());
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
