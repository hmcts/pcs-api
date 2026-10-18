package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskAssessmentUpdateEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.RiskAssessmentUpdateRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.ClaimantRiskAnswersMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("deprecation")
class ClaimantRiskAssessmentServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-03-06T10:00:00Z");

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private RiskAssessmentUpdateRepository riskAssessmentUpdateRepository;
    @Mock
    private EnforcementWaTaskService enforcementWaTaskService;

    private ClaimantRiskAssessmentService underTest;
    private EnforcementOrderEntity warrant;

    @BeforeEach
    void setUp() {
        underTest = new ClaimantRiskAssessmentService(currentEnforcementService, riskAssessmentUpdateRepository,
                                                      new ClaimantRiskAnswersMapper(), enforcementWaTaskService,
                                                      Clock.fixed(NOW, ZoneOffset.UTC));
        warrant = EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .status(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION)
            .warrantDetails(new WarrantEntity())
            .enforcementOrder(submittedAnswers(YesNoNotSure.NO, VerticalYesNo.NO))
            .build();
    }

    @Test
    void shouldSaveTheAnswersWhenOneHasChanged() {
        givenCurrentEnforcement(warrant);
        UUID updateId = givenSaveAssignsAnId();

        EnforcementChange<Boolean> recorded = underTest.recordAnswers(
            CASE_REFERENCE, submittedAnswers(YesNoNotSure.NO, VerticalYesNo.YES));

        assertThat(recorded.change()).isTrue();
        ArgumentCaptor<RiskAssessmentUpdateEntity> saved = ArgumentCaptor.forClass(RiskAssessmentUpdateEntity.class);
        verify(riskAssessmentUpdateRepository).save(saved.capture());
        assertThat(saved.getValue().getEnforcementOrder()).isSameAs(warrant);
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getAnswers().getIsDifficultToAccessProperty()).isEqualTo(VerticalYesNo.YES);
        assertThat(saved.getValue().getAnswers().getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
        verify(enforcementWaTaskService).riskAnswersChanged(CASE_REFERENCE, updateId);
    }

    @Test
    void shouldSaveNothingWhenNoAnswerHasChanged() {
        givenCurrentEnforcement(warrant);

        EnforcementChange<Boolean> recorded =
            underTest.recordAnswers(CASE_REFERENCE, submittedAnswers(YesNoNotSure.NO, VerticalYesNo.NO));

        assertThat(recorded.change()).isFalse();
        verify(riskAssessmentUpdateRepository, never()).save(any());
        verifyNoInteractions(enforcementWaTaskService);
    }

    @Test
    void shouldCompareWithTheLatestUpdateRatherThanTheApplication() {
        givenCurrentEnforcement(warrant);
        ClaimantRiskAnswers latest = ClaimantRiskAnswers.builder()
            .anyRiskToBailiff(YesNoNotSure.NO)
            .vulnerablePeoplePresent(YesNoNotSure.NO)
            .isDifficultToAccessProperty(VerticalYesNo.YES)
            .build();
        when(riskAssessmentUpdateRepository.findFirstByEnforcementOrderIdOrderByUpdatedAtDesc(warrant.getId()))
            .thenReturn(Optional.of(RiskAssessmentUpdateEntity.builder().answers(latest).build()));

        EnforcementChange<Boolean> recorded = underTest.recordAnswers(
            CASE_REFERENCE, submittedAnswers(YesNoNotSure.NO, VerticalYesNo.YES));

        assertThat(recorded.change()).isFalse();
        verify(riskAssessmentUpdateRepository, never()).save(any());
        verifyNoInteractions(enforcementWaTaskService);
    }

    @Test
    void shouldSaveNothingWhenTheClaimHasNoApprovedLiveWarrant() {
        warrant.setStatus(EnforcementStatus.AWAITING_REVIEW);
        givenCurrentEnforcement(warrant);

        EnforcementChange<Boolean> recorded = underTest.recordAnswers(
            CASE_REFERENCE, submittedAnswers(YesNoNotSure.YES, VerticalYesNo.NO));

        assertThat(recorded.change()).isFalse();
        verifyNoInteractions(riskAssessmentUpdateRepository, enforcementWaTaskService);
    }

    @Test
    void shouldSaveNothingWhenNoRiskAnswersWereSubmitted() {
        EnforcementChange<Boolean> recorded =
            underTest.recordAnswers(CASE_REFERENCE, EnforcementOrder.builder().build());

        assertThat(recorded.change()).isFalse();
        verifyNoInteractions(currentEnforcementService, riskAssessmentUpdateRepository, enforcementWaTaskService);
    }

    /**
     * The claimant is asked to try again rather than given an unexplained 500, and the answers they changed
     * are rolled back with the task request.
     */
    @Test
    void shouldRefuseTheAnswersWhenTheBailiffsTaskCannotBeRequested() {
        givenCurrentEnforcement(warrant);
        givenSaveAssignsAnId();
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementWaTaskService).riskAnswersChanged(anyLong(), any());

        EnforcementChange<Boolean> recorded = underTest.recordAnswers(
            CASE_REFERENCE, submittedAnswers(YesNoNotSure.NO, VerticalYesNo.YES));

        assertThat(recorded.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
    }

    @Test
    void shouldPrefillTheCurrentAnswers() {
        givenCurrentEnforcement(warrant);
        EnforcementOrder eventData = EnforcementOrder.builder().build();

        underTest.prefillAnswers(CASE_REFERENCE, eventData);

        assertThat(eventData.getChooseEnforcementType().getValueCode()).isEqualTo("WARRANT");
        assertThat(eventData.getWarrantDetails().getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
        assertThat(eventData.getWarrantDetails().getPropertyAccessDetails().getIsDifficultToAccessProperty())
            .isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldHideTheRiskPagesWhenThereIsNoWarrant() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());
        EnforcementOrder eventData = submittedAnswers(YesNoNotSure.YES, VerticalYesNo.YES);

        underTest.prefillAnswers(CASE_REFERENCE, eventData);

        assertThat(eventData.getChooseEnforcementType()).isNull();
        assertThat(eventData.getWarrantDetails()).isNull();
    }

    /**
     * The update row's id keys the task, and the database assigns it on save.
     */
    private UUID givenSaveAssignsAnId() {
        UUID id = UUID.randomUUID();
        when(riskAssessmentUpdateRepository.save(any())).thenAnswer(invocation -> {
            RiskAssessmentUpdateEntity update = invocation.getArgument(0);
            update.setId(id);
            return update;
        });
        return id;
    }

    private void givenCurrentEnforcement(EnforcementOrderEntity enforcement) {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
    }

    private static EnforcementOrder submittedAnswers(YesNoNotSure anyRisk, VerticalYesNo difficultToAccess) {
        DynamicStringListElement warrantType = new DynamicStringListElement("WARRANT", "Warrant of possession");
        return EnforcementOrder.builder()
            .chooseEnforcementType(new DynamicStringList(warrantType, List.of(warrantType)))
            .warrantDetails(WarrantDetails.builder()
                .anyRiskToBailiff(anyRisk)
                .propertyAccessDetails(PropertyAccessDetails.builder()
                    .isDifficultToAccessProperty(difficultToAccess)
                    .build())
                .build())
            .rawWarrantDetails(RawWarrantDetails.builder().vulnerablePeoplePresent(YesNoNotSure.NO).build())
            .build();
    }
}
