package uk.gov.hmcts.reform.pcs.hearings.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmcHearingRetrievalHandlerTest {

    @Mock
    private HmcHearingService hmcHearingService;

    private HmcHearingRetrievalHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HmcHearingRetrievalHandler(hmcHearingService, "deployment-pcs-api-pr-1",
                                                 "pcs-api-pr-1-servicebus-pcs");
    }

    @Test
    void shouldRetrieveHearingFromHmc() {
        // given
        HmcHearingResponse notification = new HmcHearingResponse();
        notification.setHearingID("2000000001");

        // when
        handler.handle(notification);

        // then
        verify(hmcHearingService).getHearing("2000000001");
    }

    @Test
    void shouldPropagateHmcFailure() {
        // given
        HmcHearingResponse notification = new HmcHearingResponse();
        notification.setHearingID("2000000001");
        when(hmcHearingService.getHearing("2000000001")).thenThrow(new IllegalStateException("HMC unavailable"));

        // when
        // then
        assertThatThrownBy(() -> handler.handle(notification))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldStartWhenDeploymentIdIsConfigured() {
        // given
        // when
        // then
        assertThatCode(handler::validateActivation).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldRefuseToStartWithoutDeploymentId(String deploymentId) {
        // given
        HmcHearingRetrievalHandler sharedEnvironmentHandler =
            new HmcHearingRetrievalHandler(hmcHearingService, deploymentId, "pcs-api-pr-1-servicebus-pcs");

        // when
        // then
        assertThatThrownBy(sharedEnvironmentHandler::validateActivation)
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hmc-to-pcs-subscription-aat", "hmc-to-pcs-subscription-prod"})
    void shouldRefuseToStartOnSharedSubscription(String subscription) {
        // given
        HmcHearingRetrievalHandler sharedSubscriptionHandler =
            new HmcHearingRetrievalHandler(hmcHearingService, "deployment-pcs-api-pr-1", subscription);

        // when
        // then
        assertThatThrownBy(sharedSubscriptionHandler::validateActivation)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(subscription);
    }
}
