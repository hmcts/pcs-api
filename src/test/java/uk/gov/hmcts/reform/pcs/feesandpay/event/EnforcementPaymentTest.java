package uk.gov.hmcts.reform.pcs.feesandpay.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class EnforcementPaymentTest extends BaseEventTest {

    @BeforeEach
    void setUp() {
        setEventUnderTest(new EnforcementPayment());
    }

    @Test
    void shouldRemainCaseIssued() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());
        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
    }

}
