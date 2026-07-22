package uk.gov.hmcts.reform.pcs.ccd.event.changecasestate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseStateOption;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class ChangeCaseStateTest extends BaseEventTest {

    private static final String FORMATTED_ADDRESS = "1 Test Street, London, SW1A 1AA";

    @Mock
    private AddressFormatter addressFormatter;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private ChangeCaseState changeCaseState;

    @BeforeEach
    void setUp() {
        setEventUnderTest(changeCaseState);
        when(addressFormatter.formatShortAddress(isNull(), eq(COMMA_DELIMITER)))
            .thenReturn(FORMATTED_ADDRESS);
    }

    @ParameterizedTest
    @EnumSource(CaseStateOption.class)
    void shouldTransitionToSelectedTargetState(CaseStateOption targetState) {
        PCSCase pcsCase = PCSCase.builder()
            .targetState(targetState)
            .build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getState()).isEqualTo(targetState.toState());
    }

    @Test
    void shouldIncludeCaseReferenceInConfirmationBody() {
        PCSCase pcsCase = PCSCase.builder()
            .targetState(CaseStateOption.JUDICIAL_REFERRAL)
            .build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getConfirmationBody())
            .contains(String.valueOf(TEST_CASE_REFERENCE));
    }

    @Test
    void shouldIncludeFormattedAddressInConfirmationBody() {
        PCSCase pcsCase = PCSCase.builder()
            .targetState(CaseStateOption.HEARING_READINESS)
            .build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getConfirmationBody()).contains(FORMATTED_ADDRESS);
    }
}
