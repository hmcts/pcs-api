package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;

class ExtRespondPossessionClaimTest extends BaseEventTest {

    ExtRespondPossessionClaim extRespondPossessionClaim;

    @BeforeEach
    void setUp() {
        extRespondPossessionClaim = new ExtRespondPossessionClaim();
        setEventUnderTest(extRespondPossessionClaim);
    }

    @Test
    void shouldReturnDefaultSubmitResponse() {
        // given
        PCSCase pcsCase = PCSCase.builder().build();

        // when
        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        // then
        assertThat(submitResponse).isEqualTo(SubmitResponse.defaultResponse());
    }

}
