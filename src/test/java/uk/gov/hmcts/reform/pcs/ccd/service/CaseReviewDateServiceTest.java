package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseReviewDateServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private Clock utcClock;

    @InjectMocks
    private CaseReviewDateService caseReviewDateService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-17T10:15:30Z");

    @BeforeEach
    void setUp() {
        when(utcClock.instant()).thenReturn(FIXED_INSTANT);
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

        PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(reviewDate1))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder().name("Case Worker").build());

        // When
        caseReviewDateService.addCaseReviewDate(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getReviewDates()).hasSize(1);
        CaseReviewDateEntity caseReviewDateEntity = persistedCaseEntity.getReviewDates().getFirst();
        assertThat(caseReviewDateEntity.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(caseReviewDateEntity.getReason()).isEqualTo(ReviewReason.DISMISS_CASE);
        assertThat(caseReviewDateEntity.getDescription()).isEqualTo("review description 1");
        assertThat(caseReviewDateEntity.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity.getCreatedOn()).isEqualTo(FIXED_INSTANT);
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

        PCSCase pcsCase = PCSCase.builder()
            .reviewDates(List.of(reviewDate1, reviewDate2))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder().name("Case Worker").build());

        // When
        caseReviewDateService.addCaseReviewDate(caseReference, pcsCase);

        // Then
        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getReviewDates()).hasSize(2);

        CaseReviewDateEntity caseReviewDateEntity1 = persistedCaseEntity.getReviewDates().getFirst();
        assertThat(caseReviewDateEntity1.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity1.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(caseReviewDateEntity1.getReason()).isEqualTo(ReviewReason.DISMISS_CASE);
        assertThat(caseReviewDateEntity1.getDescription()).isEqualTo("review description 1");
        assertThat(caseReviewDateEntity1.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity1.getCreatedOn()).isEqualTo(FIXED_INSTANT);

        CaseReviewDateEntity caseReviewDateEntity2 = persistedCaseEntity.getReviewDates().getLast();
        assertThat(caseReviewDateEntity2.getPcsCase()).isEqualTo(persistedCaseEntity);
        assertThat(caseReviewDateEntity2.getDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(caseReviewDateEntity2.getReason()).isEqualTo(ReviewReason.OTHER);
        assertThat(caseReviewDateEntity2.getDescription()).isEqualTo("review description 2");
        assertThat(caseReviewDateEntity2.getCreatedBy()).isEqualTo("Case Worker");
        assertThat(caseReviewDateEntity2.getCreatedOn()).isEqualTo(FIXED_INSTANT);
    }
}
