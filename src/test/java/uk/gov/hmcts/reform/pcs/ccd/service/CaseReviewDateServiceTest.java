package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class CaseReviewDateServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private CamundaService camundaService;

    @Mock
    private TaskDescriptionService taskDescriptionService;

    @InjectMocks
    private CaseReviewDateService caseReviewDateService;

    @Test
    void shouldAddCaseReviewDate() {
        // Given
        ListValue<ReviewDate> reviewDate1 = ListValue.<ReviewDate>builder()
            .value(
                ReviewDate.builder()
                    .date(LocalDate.of(2026, 2, 1))
                    .reason(ReviewReason.DISMISS_CASE)
                    .description("review description 1")
                    .build()
            ).build();

        PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(reviewDate1))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 1)).thenReturn("task description");

        // When
        caseReviewDateService.addCaseReviewDates(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        Instant taskCreationTime = LocalDateTime.of(2026, 2, 1, 0, 0, 0).atZone(UK_ZONE_ID).toInstant();
        verify(camundaService).createTask(
            caseReference,
            TaskType.REVIEW_DATE_DUE,
            "task description",
            taskCreationTime
        );

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getReviewDates()).hasSize(1);
        CaseReviewDateEntity caseReviewDateEntity = persistedCaseEntity.getReviewDates().getFirst();
        assertThat(caseReviewDateEntity.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(caseReviewDateEntity.getReason()).isEqualTo(ReviewReason.DISMISS_CASE);
        assertThat(caseReviewDateEntity.getDescription()).isEqualTo("review description 1");
    }

    @Test
    void shouldAddMultipleReviewDates() {
        // Given
        ListValue<ReviewDate> reviewDate1 = ListValue.<ReviewDate>builder()
            .value(
                ReviewDate.builder()
                    .date(LocalDate.of(2026, 2, 1))
                    .reason(ReviewReason.DISMISS_CASE)
                    .description("review description 1")
                    .build()
            ).build();

        ListValue<ReviewDate> reviewDate2 = ListValue.<ReviewDate>builder()
            .value(
                ReviewDate.builder()
                    .date(LocalDate.of(2026, 3, 2))
                    .reason(ReviewReason.OTHER)
                    .description("review description 2")
                    .build()
            ).build();

        final PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(reviewDate1, reviewDate2))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 1)).thenReturn("task description 1");
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 2)).thenReturn("task description 2");

        // When
        caseReviewDateService.addCaseReviewDates(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        Instant taskCreationTime1 = LocalDateTime.of(2026, 2, 1, 0, 0, 0).atZone(UK_ZONE_ID).toInstant();
        verify(camundaService).createTask(
            caseReference,
            TaskType.REVIEW_DATE_DUE,
            "task description 1",
            taskCreationTime1
        );

        Instant taskCreationTime2 = LocalDateTime.of(2026, 3, 2, 0, 0, 0).atZone(UK_ZONE_ID).toInstant();
        verify(camundaService).createTask(
            caseReference,
            TaskType.REVIEW_DATE_DUE,
            "task description 2",
            taskCreationTime2
        );

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getReviewDates()).hasSize(2);

        CaseReviewDateEntity caseReviewDateEntity1 = persistedCaseEntity.getReviewDates().getFirst();
        assertThat(caseReviewDateEntity1.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity1.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(caseReviewDateEntity1.getReason()).isEqualTo(ReviewReason.DISMISS_CASE);
        assertThat(caseReviewDateEntity1.getDescription()).isEqualTo("review description 1");

        CaseReviewDateEntity caseReviewDateEntity2 = persistedCaseEntity.getReviewDates().getLast();
        assertThat(caseReviewDateEntity2.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity2.getDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(caseReviewDateEntity2.getReason()).isEqualTo(ReviewReason.OTHER);
        assertThat(caseReviewDateEntity2.getDescription()).isEqualTo("review description 2");
    }

    @Test
    void shouldUseNextReviewDateNumberWhenCaseAlreadyHasReviewDates() {
        // Given
        ListValue<ReviewDate> existingReviewDate = ListValue.<ReviewDate>builder()
            .id("existing-review-date-id")
            .value(
                ReviewDate.builder()
                    .date(LocalDate.of(2026, 1, 1))
                    .reason(ReviewReason.DISMISS_CASE)
                    .description("existing review description")
                    .build()
            ).build();

        ListValue<ReviewDate> reviewDate = ListValue.<ReviewDate>builder()
            .id("new-review-date-id")
            .value(
                ReviewDate.builder()
                    .date(LocalDate.of(2026, 4, 3))
                    .reason(ReviewReason.OTHER)
                    .description("new review description")
                    .build()
            ).build();

        final PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(existingReviewDate, reviewDate))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.addCaseReviewDate(CaseReviewDateEntity.builder()
                                        .date(LocalDate.of(2026, 1, 1))
                                        .reason(ReviewReason.DISMISS_CASE)
                                        .description("existing review description")
                                        .build());

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 2)).thenReturn("task description 2");

        // When
        caseReviewDateService.addCaseReviewDates(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        Instant taskCreationTime = LocalDateTime.of(2026, 4, 3, 0, 0, 0).atZone(UK_ZONE_ID).toInstant();
        verify(camundaService).createTask(
            caseReference,
            TaskType.REVIEW_DATE_DUE,
            "task description 2",
            taskCreationTime
        );

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getReviewDates()).hasSize(2);
        CaseReviewDateEntity newCaseReviewDateEntity = persistedCaseEntity.getReviewDates().getLast();
        assertThat(newCaseReviewDateEntity.getDate()).isEqualTo(LocalDate.of(2026, 4, 3));
        assertThat(newCaseReviewDateEntity.getReason()).isEqualTo(ReviewReason.OTHER);
        assertThat(newCaseReviewDateEntity.getDescription()).isEqualTo("new review description");
    }
}
