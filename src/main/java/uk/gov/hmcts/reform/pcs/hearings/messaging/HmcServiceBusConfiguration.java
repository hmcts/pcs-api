package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcServiceBusConfiguration {

    static final String PCS_PRODUCT = "pcs";

    private static final Pattern SERVICE_SUBSCRIPTION = Pattern.compile("^hmc-to-(.+)-subscription-.+$");

    @Bean(destroyMethod = "close")
    public ServiceBusProcessorClient hmcHearingsProcessorClient(
        @Value("${hmc.service-bus.connection-string}") String connectionString,
        @Value("${hmc.service-bus.topic}") String topic,
        @Value("${hmc.service-bus.subscription}") String subscription,
        @Value("${hmc.service-bus.max-concurrent-calls}") int maxConcurrentCalls,
        @Value("${hmc.service-bus.max-delivery-count}") int maxDeliveryCount,
        @Value("${hmc.deployment-id}") String deploymentId,
        HmcHearingNotificationListener listener) {

        validate(connectionString, topic, subscription, maxConcurrentCalls, maxDeliveryCount);
        log.info("HMC Service Bus consumer configured for {}/{} with deployment validation {}", topic, subscription,
                 hasText(deploymentId) ? "restricted to " + deploymentId : "off");

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

    private static void validate(String connectionString, String topic, String subscription,
                                 int maxConcurrentCalls, int maxDeliveryCount) {
        if (!hasText(connectionString)) {
            throw new IllegalStateException("hmc.service-bus.connection-string is required when HMC Service Bus "
                                                + "consumption is enabled");
        }
        if (!hasText(topic)) {
            throw new IllegalStateException("hmc.service-bus.topic is required when HMC Service Bus consumption "
                                                + "is enabled");
        }
        if (!hasText(subscription)) {
            throw new IllegalStateException("hmc.service-bus.subscription is required when HMC Service Bus "
                                                + "consumption is enabled");
        }
        Matcher serviceSubscription = SERVICE_SUBSCRIPTION.matcher(subscription);
        if (serviceSubscription.matches() && !PCS_PRODUCT.equals(serviceSubscription.group(1))) {
            throw new IllegalStateException("hmc.service-bus.subscription " + subscription + " belongs to the "
                                                + serviceSubscription.group(1) + " service and must not be consumed");
        }
        if (maxConcurrentCalls < 1) {
            throw new IllegalStateException("hmc.service-bus.max-concurrent-calls must be at least 1");
        }
        if (maxDeliveryCount < 1) {
            throw new IllegalStateException("hmc.service-bus.max-delivery-count must be at least 1");
        }
    }
}
