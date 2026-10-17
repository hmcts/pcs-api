package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.ConfirmEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestSupportEnvironment;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantEvictionConfirmationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfirmEvictionTest extends BaseEventTest {

    @InjectMocks
    private ConfirmEviction underTest;
    @Mock
    private ConfirmEvictionConfigurer confirmEvictionConfigurer;
    @Mock
    private ClaimantEvictionConfirmationService claimantEvictionConfirmationService;

    @Test
    void shouldConfigureDecentralisedWhenNonProdSupportEnabled() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        try (MockedStatic<TestSupportEnvironment> mocked = mockStatic(TestSupportEnvironment.class)) {
            mocked.when(TestSupportEnvironment::isDev).thenReturn(true);
            setEventUnderTest(underTest);

            // When
            callSubmitHandler(caseData);

            // Then
            verify(confirmEvictionConfigurer).configurePages(any(PageBuilder.class));
        }
    }

    @Test
    void shouldRecordTheClaimantsAttendanceAnswerOnSubmit() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().claimantCanAttendEviction(YesOrNo.NO).build())
            .build();
        setEventUnderTest(underTest);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(claimantEvictionConfirmationService).recordAttendance(TEST_CASE_REFERENCE, YesOrNo.NO);
    }

    @Test
    void shouldRecordNoAnswerWhenTheCaseHasNoEnforcementOrder() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        setEventUnderTest(underTest);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(claimantEvictionConfirmationService).recordAttendance(TEST_CASE_REFERENCE, null);
    }

}
