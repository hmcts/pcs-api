package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class CaseReviewDateServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private Clock ukClock;

    @Mock
    private CamundaService camundaService;

    @Mock
    private TaskDescriptionService taskDescriptionService;

    @InjectMocks
    private CaseReviewDateService caseReviewDateService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-17T10:15:30Z");
    private static final LocalDateTime FIXED_UK_DATE_TIME = LocalDateTime.of(2026, 8, 17, 11, 15, 30);

    @BeforeEach
    void setUp() {
        lenient().when(ukClock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(ukClock.getZone()).thenReturn(UK_ZONE_ID);
    }

    @Test
    void shouldNotAddCaseReviewDatesWhenReviewDatesAreNull() {
        // Given
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(PcsCaseEntity.builder().build());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseReviewDateService.addCaseReviewDates(caseReference, pcsCase);

        // Then
        verify(pcsCaseService).loadCase(caseReference);
        verifyNoInteractions(
            pcsCaseRepository,
            securityContextService,
            camundaService,
            taskDescriptionService
        );
    }

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

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder().name("Case Worker").build());
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 1)).thenReturn("task description");

        // When
        PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(reviewDate1))
            .build();
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
        assertThat(caseReviewDateEntity.getReviewDateNumber()).isEqualTo(1);
        assertThat(caseReviewDateEntity.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(caseReviewDateEntity.getReason()).isEqualTo(ReviewReason.DISMISS_CASE);
        assertThat(caseReviewDateEntity.getDescription()).isEqualTo("review description 1");
        assertThat(caseReviewDateEntity.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity.getCreatedDate()).isEqualTo(FIXED_UK_DATE_TIME);
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

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder().name("Case Worker").build());
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 1)).thenReturn("task description 1");
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 2)).thenReturn("task description 2");

        // When
        PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(reviewDate1, reviewDate2))
            .build();
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
        assertThat(caseReviewDateEntity1.getReviewDateNumber()).isEqualTo(1);
        assertThat(caseReviewDateEntity1.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(caseReviewDateEntity1.getReason()).isEqualTo(ReviewReason.DISMISS_CASE);
        assertThat(caseReviewDateEntity1.getDescription()).isEqualTo("review description 1");
        assertThat(caseReviewDateEntity1.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity1.getCreatedDate()).isEqualTo(FIXED_UK_DATE_TIME);

        CaseReviewDateEntity caseReviewDateEntity2 = persistedCaseEntity.getReviewDates().getLast();
        assertThat(caseReviewDateEntity2.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity2.getReviewDateNumber()).isEqualTo(2);
        assertThat(caseReviewDateEntity2.getDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(caseReviewDateEntity2.getReason()).isEqualTo(ReviewReason.OTHER);
        assertThat(caseReviewDateEntity2.getDescription()).isEqualTo("review description 2");
        assertThat(caseReviewDateEntity2.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity2.getCreatedDate()).isEqualTo(FIXED_UK_DATE_TIME);
    }

    @Test
    void shouldContinueReviewDateRankFromExistingRows() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.addCaseReviewDate(
            CaseReviewDateEntity.builder()
                .date(LocalDate.of(2026, 2, 1))
                .reason(ReviewReason.DISMISS_CASE)
                .description("existing review description")
                .build()
        );
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder().name("Case Worker").build());
        when(taskDescriptionService.createReviewDueDateDescription(caseReference, 2)).thenReturn("task description");

        ListValue<ReviewDate> newReviewDate = ListValue.<ReviewDate>builder()
            .value(
                ReviewDate.builder()
                    .date(LocalDate.of(2026, 3, 2))
                    .reason(ReviewReason.OTHER)
                    .description("new review description")
                    .build()
            ).build();

        PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(newReviewDate))
            .build();

        // When
        caseReviewDateService.addCaseReviewDates(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        Instant taskCreationTime = LocalDateTime.of(2026, 3, 2, 0, 0, 0).atZone(UK_ZONE_ID).toInstant();
        verify(camundaService).createTask(
            caseReference,
            TaskType.REVIEW_DATE_DUE,
            "task description",
            taskCreationTime
        );

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getReviewDates()).hasSize(2);
        CaseReviewDateEntity caseReviewDateEntity = persistedCaseEntity.getReviewDates().getLast();
        assertThat(caseReviewDateEntity.getReviewDateNumber()).isEqualTo(2);
        assertThat(caseReviewDateEntity.getDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(caseReviewDateEntity.getReason()).isEqualTo(ReviewReason.OTHER);
        assertThat(caseReviewDateEntity.getDescription()).isEqualTo("new review description");
        assertThat(caseReviewDateEntity.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity.getCreatedDate()).isEqualTo(FIXED_UK_DATE_TIME);
    }
}
