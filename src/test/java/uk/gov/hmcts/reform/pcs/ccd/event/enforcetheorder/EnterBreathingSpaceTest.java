package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.BreathingSpacePageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterBreathingSpaceTest extends BaseEventTest {

    @Mock
    private BreathingSpacePageConfigurer breathingSpacePageConfigurer;
    @Mock
    private CaseNameFormatter caseNameFormatter;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new EnterBreathingSpace(breathingSpacePageConfigurer, caseNameFormatter));
    }

    @Test
    void shouldSetCaseNameOnStart() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(caseNameFormatter.formatCaseName(caseData)).thenReturn("Claimant Ltd vs John Smith");

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getBreathingSpaceDetails().getCaseName()).isEqualTo("Claimant Ltd vs John Smith");
    }

}
