package uk.gov.hmcts.reform.pcs.ccd.event.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.CaseReindexingService;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.maintenance.ReindexCasesDetails;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.maintenance.reindexcases.ReindexCasesCriteria;

import java.time.LocalDate;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReindexCasesTest extends BaseEventTest {

    @Mock
    private CaseReindexingService reindexQueueService;
    @Mock
    private ReindexCasesCriteria reindexCasesCriteria;


    @BeforeEach
    void setUp() {
        ReindexCases underTest = new ReindexCases(reindexQueueService, reindexCasesCriteria);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldEnqueueCasesForReIndexing() {
        // Given
        LocalDate expectedSinceDate = LocalDate.of(2025, JUNE, 10);
        int expectedCaseCount = 10;

        PCSCase pcsCase = PCSCase.builder()
            .reindexCases(ReindexCasesDetails.builder()
                              .modifiedSince(expectedSinceDate)
                              .build())
            .build();

        when(reindexQueueService.enqueueCasesModifiedSince(expectedSinceDate)).thenReturn(expectedCaseCount);

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        // Then
        verify(reindexQueueService).enqueueCasesModifiedSince(expectedSinceDate);
        assertThat(submitResponse.getConfirmationBody())
            .contains("10 cases modified since 10 June 2025 have been queued for re-indexing");

    }

}
