package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmcServiceBusActivationTest {

    @Mock
    private ServiceBusProcessorClient processorClient;
    @Mock
    private FeatureToggleService featureToggleService;

    private HmcServiceBusActivation activation;

    @BeforeEach
    void setUp() {
        activation = new HmcServiceBusActivation(processorClient, featureToggleService);
        lenient().when(processorClient.getTopicName()).thenReturn("hmc-to-cft-aat");
        lenient().when(processorClient.getSubscriptionName()).thenReturn("pcs-api-pr-1-servicebus-pcs");
    }

    @Test
    void shouldStartConsumerWhenFlagIsEnabledAndConsumerIsStopped() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(true);
        when(processorClient.isRunning()).thenReturn(false);

        // when
        activation.reconcile();

        // then
        verify(processorClient).start();
        verify(processorClient, never()).stop();
    }

    @Test
    void shouldLeaveConsumerRunningWhenFlagIsEnabledAndConsumerIsRunning() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(true);
        when(processorClient.isRunning()).thenReturn(true);

        // when
        activation.reconcile();

        // then
        verify(processorClient, never()).start();
        verify(processorClient, never()).stop();
    }

    @Test
    void shouldStopConsumerWhenFlagIsDisabledAndConsumerIsRunning() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(false);
        when(processorClient.isRunning()).thenReturn(true);

        // when
        activation.reconcile();

        // then
        verify(processorClient).stop();
        verify(processorClient, never()).start();
    }

    @Test
    void shouldNotStartConsumerWhenFlagIsDisabled() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(false);
        when(processorClient.isRunning()).thenReturn(false);

        // when
        activation.reconcile();

        // then
        verify(processorClient, never()).start();
        verify(processorClient, never()).stop();
    }
}
