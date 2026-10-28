package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcServiceBusConfiguration {

    @Bean(initMethod = "start", destroyMethod = "close")
    public ServiceBusProcessorClient hmcHearingsProcessorClient(
        @Value("${hmc.service-bus.connection-string}") String connectionString,
        @Value("${hmc.service-bus.topic}") String topic,
        @Value("${hmc.service-bus.subscription}") String subscription,
        @Value("${hmc.service-bus.max-concurrent-calls}") int maxConcurrentCalls,
        HmcHearingNotificationListener listener) {

        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .processor()
            .topicName(topic)
            .subscriptionName(subscription)
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .disableAutoComplete()
            .maxConcurrentCalls(maxConcurrentCalls)
            .processMessage(listener::onMessage)
            .processError(listener::onError)
            .buildProcessorClient();
    }
}
