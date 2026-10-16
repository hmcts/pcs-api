package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseGenerationContext;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.event.EnforceTheOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementTestCaseGeneratorTest {

    @InjectMocks
    private EnforcementTestCaseGenerator underTest;

    @Mock
    private EnforceTheOrder enforceTheOrder;
    @Mock
    private TestCaseGenerationContext context;

    @Test
    void shouldHandleEnforcementFixtureLabels() {
        assertThat(underTest.handles("Create Enforcement Warrant Basic Case")).isTrue();
        assertThat(underTest.handles("Create Enforcement Writ Basic Case")).isTrue();
    }

    @Test
    void shouldNotHandleOtherFixtureLabels() {
        assertThat(underTest.handles("Create Case Make A Claim Basic Case")).isFalse();
        assertThat(underTest.handles("Please select ...")).isFalse();
    }

    @Test
    void shouldCreateBasicClaimThenSubmitEnforcementOrder() {
        // Given
        long caseReference = 456L;
        String label = "Create Enforcement Warrant Basic Case";
        PCSCase loadedCase = PCSCase.builder().build();
        when(context.loadTestPcsCase(label)).thenReturn(loadedCase);

        // When
        State state = underTest.generate(label, caseReference, context);

        // Then
        assertThat(state).isEqualTo(State.CASE_ISSUED);
        InOrder inOrder = inOrder(context, enforceTheOrder);
        inOrder.verify(context).makeAClaimTestCreation("Create-Case-Make-A-Claim-Basic-Case", caseReference);
        inOrder.verify(enforceTheOrder).submit(new EventPayload<>(caseReference, loadedCase, null));
    }

}
