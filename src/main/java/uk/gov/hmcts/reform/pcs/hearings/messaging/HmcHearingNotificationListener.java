package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;

import java.io.IOException;
import java.util.Objects;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcHearingNotificationListener {

    static final String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";
    static final String UNREADABLE_REASON = "UnreadableHmcNotification";
    static final String MISSING_HEARING_ID_REASON = "MissingHearingId";

    private final ObjectMapper objectMapper;
    private final HmcHearingNotificationProcessor processor;
    private final String deploymentId;

    public HmcHearingNotificationListener(ObjectMapper objectMapper,
                                          HmcHearingNotificationProcessor processor,
                                          @Value("${hmc.deployment-id}") String deploymentId) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.deploymentId = deploymentId;
    }

    public void onMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();

        if (!isForThisDeployment(message)) {
            log.info("Completing HMC notification {} addressed to a different deployment", message.getMessageId());
            context.complete();
            return;
        }

        HmcHearingResponse notification;
        try {
            notification = objectMapper.readValue(message.getBody().toBytes(), HmcHearingResponse.class);
        } catch (IOException e) {
            log.error("Dead-lettering unreadable HMC notification {}", message.getMessageId(), e);
            context.deadLetter(deadLetterOptions(UNREADABLE_REASON, e.getMessage()));
            return;
        }

        if (!hasText(notification.getHearingID())) {
            log.error("Dead-lettering HMC notification {} without a hearing id", message.getMessageId());
            context.deadLetter(deadLetterOptions(MISSING_HEARING_ID_REASON, "Notification has no hearingID"));
            return;
        }

        try {
            processor.process(notification);
        } catch (RuntimeException e) {
            log.error("Abandoning HMC notification {} for hearing {}", message.getMessageId(),
                      notification.getHearingID(), e);
            context.abandon();
            return;
        }

        context.complete();
    }

    public void onError(ServiceBusErrorContext context) {
        log.error("HMC Service Bus error from {} during {}", context.getEntityPath(), context.getErrorSource(),
                  context.getException());
    }

    private boolean isForThisDeployment(ServiceBusReceivedMessage message) {
        Object messageDeploymentId = message.getApplicationProperties().get(HMCTS_DEPLOYMENT_ID);
        return Objects.equals(normalise(messageDeploymentId), normalise(deploymentId));
    }

    private static String normalise(Object value) {
        return value instanceof String text && hasText(text) ? text : null;
    }

    private static DeadLetterOptions deadLetterOptions(String reason, String description) {
        return new DeadLetterOptions()
            .setDeadLetterReason(reason)
            .setDeadLetterErrorDescription(description);
    }
}
