package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAssessmentDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskAssessmentUpdateEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantRiskAssessmentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimantRiskAssessmentViewTest {

    private static final ClaimantRiskAnswers ORIGINAL = ClaimantRiskAnswers.builder()
        .anyRiskToBailiff(YesNoNotSure.NO)
        .vulnerablePeoplePresent(YesNoNotSure.NO)
        .isDifficultToAccessProperty(VerticalYesNo.NO)
        .additionalInformationSelect(VerticalYesNo.NO)
        .build();
    private static final ClaimantRiskAnswers UPDATED = ORIGINAL.toBuilder()
        .anyRiskToBailiff(YesNoNotSure.YES)
        .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
        .violentDetails("Threw a chair")
        .build();
    private static final Instant UPDATED_AT = Instant.parse("2026-03-06T10:00:00Z");

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private ClaimantRiskAssessmentService claimantRiskAssessmentService;

    private ClaimantRiskAssessmentView underTest;
    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
    private EnforcementOrderEntity warrant;

    @BeforeEach
    void setUp() {
        underTest = new ClaimantRiskAssessmentView(currentEnforcementService, claimantRiskAssessmentService);
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        // 23:30 UTC is already the next day in the UK in summer.
        statementOfTruth.setCompletedDate(LocalDateTime.parse("2026-06-24T23:30:00"));
        warrant = EnforcementOrderEntity.builder()
            .status(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION)
            .warrantDetails(new WarrantEntity())
            .statementOfTruth(statementOfTruth)
            .build();
    }

    @Test
    void shouldShowTheOriginalAnswersWithNoUpdatedAnswersBeforeAnyChange() {
        givenWarrant(ORIGINAL, null);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        ClaimantRiskAssessmentDetails details = pcsCase.getEnforcementOrder().getClaimantRiskAssessment();
        assertThat(rows(details)).extracting(ClaimantRiskAnswer::getQuestion).containsExactly(
            "Does anyone living at the property pose a risk to the bailiff?",
            "Is anyone living at the property vulnerable?",
            "Is it difficult to access the property?",
            "Do you want to tell us anything else that could help with the eviction?");
        assertThat(rows(details)).extracting(ClaimantRiskAnswer::getUpdatedAnswer).containsOnlyNulls();
        assertThat(rows(details)).extracting(ClaimantRiskAnswer::getAnswerChanged).containsOnly(YesOrNo.NO);
        assertThat(details.getClaimantRiskAnswersGivenOn()).isEqualTo("25 June 2026");
        assertThat(details.getClaimantRiskAnswersUpdatedOn()).isNull();
        assertThat(details.getClaimantRiskUpdateToReview()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowOnlyTheChangedAnswersInTheUpdatedColumn() {
        givenWarrant(ORIGINAL, UPDATED);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        ClaimantRiskAssessmentDetails details = pcsCase.getEnforcementOrder().getClaimantRiskAssessment();
        assertThat(row(details, "Does anyone living at the property pose a risk to the bailiff?"))
            .extracting(ClaimantRiskAnswer::getOriginalAnswer, ClaimantRiskAnswer::getUpdatedAnswer,
                        ClaimantRiskAnswer::getAnswerChanged)
            .containsExactly("No", "Yes", YesOrNo.YES);
        assertThat(row(details, "What kind of risks do they pose to the bailiff?"))
            .extracting(ClaimantRiskAnswer::getOriginalAnswer, ClaimantRiskAnswer::getUpdatedAnswer)
            .containsExactly(ClaimantRiskAssessmentView.NOT_ANSWERED, "Violent or aggressive behaviour");
        assertThat(row(details, "How have they been violent or aggressive?").getUpdatedAnswer())
            .isEqualTo("Threw a chair");
        assertThat(row(details, "Is it difficult to access the property?"))
            .extracting(ClaimantRiskAnswer::getUpdatedAnswer, ClaimantRiskAnswer::getAnswerChanged)
            .containsExactly(null, YesOrNo.NO);
        assertThat(details.getClaimantRiskAnswersUpdatedOn()).isEqualTo("6 March 2026");
        assertThat(details.getClaimantRiskUpdateToReview()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowAnAnswerThatWasRemovedAsNotAnswered() {
        givenWarrant(UPDATED, ORIGINAL);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        ClaimantRiskAssessmentDetails details = pcsCase.getEnforcementOrder().getClaimantRiskAssessment();
        assertThat(row(details, "How have they been violent or aggressive?"))
            .extracting(ClaimantRiskAnswer::getOriginalAnswer, ClaimantRiskAnswer::getUpdatedAnswer)
            .containsExactly("Threw a chair", ClaimantRiskAssessmentView.NOT_ANSWERED);
    }

    @Test
    void shouldStopAskingForAReviewOnceTheBailiffHasSetTheRiskLevelSinceTheUpdate() {
        givenWarrant(ORIGINAL, UPDATED);
        warrant.setRiskLevelSetAt(UPDATED_AT.plusSeconds(60));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getClaimantRiskAssessment().getClaimantRiskUpdateToReview())
            .isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldAskForAReviewWhenTheUpdateCameAfterTheRiskLevelWasSet() {
        givenWarrant(ORIGINAL, UPDATED);
        warrant.setRiskLevelSetAt(UPDATED_AT.minusSeconds(60));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getClaimantRiskAssessment().getClaimantRiskUpdateToReview())
            .isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldClearTheFieldsWhenThereIsNoApprovedLiveWarrant() {
        warrant.setStatus(EnforcementStatus.AWAITING_REVIEW);
        when(currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(warrant));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .claimantRiskAssessment(ClaimantRiskAssessmentDetails.builder()
                    .claimantRiskUpdateToReview(YesOrNo.YES)
                    .build())
                .build())
            .build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getClaimantRiskAssessment()).isNull();
    }

    @Test
    void shouldLeaveACaseWithNoEnforcementWithoutAnEnforcementOrder() {
        when(currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.empty());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setClaimantRiskAssessment(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    private void givenWarrant(ClaimantRiskAnswers original, ClaimantRiskAnswers updated) {
        when(currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(warrant));
        when(claimantRiskAssessmentService.getOriginalAnswers(warrant)).thenReturn(Optional.of(original));
        when(claimantRiskAssessmentService.getLatestUpdate(warrant)).thenReturn(Optional.ofNullable(updated)
            .map(answers -> RiskAssessmentUpdateEntity.builder().answers(answers).updatedAt(UPDATED_AT).build()));
    }

    private static List<ClaimantRiskAnswer> rows(ClaimantRiskAssessmentDetails details) {
        return details.getClaimantRiskAnswers().stream().map(ListValue::getValue).toList();
    }

    private static ClaimantRiskAnswer row(ClaimantRiskAssessmentDetails details, String question) {
        return rows(details).stream()
            .filter(row -> row.getQuestion().equals(question))
            .findFirst()
            .orElseThrow();
    }
}
