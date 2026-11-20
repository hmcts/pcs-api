package uk.gov.hmcts.reform.pcs.ccd.event.confirmeviction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder.EnforcementFlagGuard;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction.ConfirmEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestSupportEnvironment;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmEvictionTest extends BaseEventTest {

    private static final String REFUSAL_EXPECTED_STRING = "This action is not available";

    @InjectMocks
    private ConfirmEviction underTest;
    @Mock
    private ConfirmEvictionConfigurer confirmEvictionConfigurer;
    @Mock
    private EnforcementFlagGuard enforcementFlagGuard;

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
    void givenEnforcementFlagOn_whenStart_thenReturnCaseDataUnchanged() {

        setEventUnderTest(underTest);
        PCSCase expectedCaseData = PCSCase.builder().build();

        PCSCase actualCaseData = callStartHandler(expectedCaseData);

        assertThat(actualCaseData).isSameAs(expectedCaseData);
    }

    @Test
    void givenEnforcementFlagOff_whenStart_thenThrowCaseAccessException() {
        setEventUnderTest(underTest);
        when(enforcementFlagGuard.isOff()).thenReturn(true);
        doCallRealMethod().when(enforcementFlagGuard).requireOnStart();
        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage(REFUSAL_EXPECTED_STRING);
    }

    @Test
    void givenEnforcementFlagOn_whenSubmit_thenReturnNoErrors() {
        setEventUnderTest(underTest);

        SubmitResponse<State> actualResponse = callSubmitHandler(PCSCase.builder().build());

        assertThat(actualResponse.getErrors()).isNullOrEmpty();
    }

    @Test
    void givenEnforcementFlagOff_whenSubmit_thenReturnRefusalError() {
        setEventUnderTest(underTest);
        when(enforcementFlagGuard.isOff()).thenReturn(true);
        when(enforcementFlagGuard.refused()).thenCallRealMethod();
        List<String> expectedErrors = List.of(REFUSAL_EXPECTED_STRING);

        SubmitResponse<State> actualResponse = callSubmitHandler(PCSCase.builder().build());

        assertThat(actualResponse.getErrors()).isEqualTo(expectedErrors);
    }
}
