package uk.gov.hmcts.reform.pcs.ccd.page.maintenance.reindexcases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.CaseReindexingService;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.maintenance.ReindexCasesDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReindexCasesCriteriaTest extends BasePageTest {

    @Mock
    private CaseReindexingService caseReindexingService;

    @BeforeEach
    void setUp() {
        ReindexCasesCriteria underTest = new ReindexCasesCriteria(caseReindexingService);

        setPageUnderTest(underTest);
    }

    @Test
    void shouldSetMatchingCount() {
        // Given
        LocalDate modifiedSince = mock(LocalDate.class);
        long expectedModifiedCaseCount = 99L;

        PCSCase caseData = PCSCase.builder()
            .reindexCases(ReindexCasesDetails.builder()
                              .modifiedSince(modifiedSince)
                              .build())
            .build();

        when(caseReindexingService.countCasesModifiedSince(modifiedSince))
            .thenReturn(expectedModifiedCaseCount);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getReindexCases().getMatchingCount())
            .isEqualTo(expectedModifiedCaseCount);
    }
}
