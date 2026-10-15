package uk.gov.hmcts.reform.pcs.ccd.event.confirmeviction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction.ConfirmEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestSupportEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfirmEvictionTest extends BaseEventTest {

    @InjectMocks
    private ConfirmEviction underTest;
    @Mock
    private ConfirmEvictionConfigurer confirmEvictionConfigurer;

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

}
