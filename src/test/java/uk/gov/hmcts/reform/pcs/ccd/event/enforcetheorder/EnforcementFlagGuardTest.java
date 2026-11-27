package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementFlagGuardTest {

    private static final String EXPECTED_MESSAGE = "This action is not available";

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private EnforcementFlagGuard enforcementFlagGuard;

    @Test
    void givenEnforcementFlagOn_whenIsOff_thenReturnFalse() {

        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        boolean actualIsOff = enforcementFlagGuard.isOff();
        assertThat(actualIsOff).isFalse();
    }

    @Test
    void givenEnforcementFlagOff_whenIsOff_thenReturnTrue() {

        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(false);
        boolean actualIsOff = enforcementFlagGuard.isOff();
        assertThat(actualIsOff).isTrue();
    }

    @Test
    void givenEnforcementFlagOn_whenRequireOnStart_thenDoNotThrow() {

        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        assertThatNoException().isThrownBy(enforcementFlagGuard::requireOnStart);
    }

    @Test
    void givenEnforcementFlagOff_whenRequireOnStart_thenThrowCaseAccessException() {

        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(false);

        assertThatThrownBy(enforcementFlagGuard::requireOnStart)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage(EXPECTED_MESSAGE);
    }

    @Test
    void givenRefusal_whenRefused_thenReturnErrorMessage() {
        List<String> expectedErrors = List.of(EXPECTED_MESSAGE);

        SubmitResponse<State> actualResponse = enforcementFlagGuard.refused();
        assertThat(actualResponse.getErrors()).isEqualTo(expectedErrors);
    }

    @Test
    void givenRefusal_whenRefused_thenLeaveStateUnchanged() {
        SubmitResponse<State> actualResponse = enforcementFlagGuard.refused();
        assertThat(actualResponse.getState()).isNull();
    }
}
