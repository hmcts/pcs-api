package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcHearingNotificationListener {

    static final String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";
    static final String HMCTS_SERVICE_ID = "hmctsServiceId";
    static final String UNREADABLE_REASON = "UnreadableHmcNotification";
    static final String INVALID_NOTIFICATION_REASON = "InvalidHmcNotification";
    static final String UNEXPECTED_SERVICE_REASON = "UnexpectedHmctsServiceCode";
    static final String DEPLOYMENT_MISMATCH_REASON = "HmctsDeploymentMismatch";
    static final String HMC_REJECTED_REASON = "HmcRejectedRequest";
    static final String RETRIES_EXHAUSTED_REASON = "HmcRetrievalRetriesExhausted";

    private static final Set<Integer> RETRYABLE_CLIENT_ERRORS = Set.of(
        HttpStatus.UNAUTHORIZED.value(),
        HttpStatus.FORBIDDEN.value(),
        HttpStatus.REQUEST_TIMEOUT.value(),
        HttpStatus.TOO_MANY_REQUESTS.value()
    );

    private final ObjectMapper objectMapper;
    private final HmcHearingNotificationHandler handler;
    private final String deploymentId;
    private final String serviceId;
    private final int maxDeliveryCount;

    public HmcHearingNotificationListener(ObjectMapper objectMapper,
                                          HmcHearingNotificationHandler handler,
                                          @Value("${hmc.deployment-id}") String deploymentId,
                                          @Value("${hmc.serviceId}") String serviceId,
                                          @Value("${hmc.service-bus.max-delivery-count}") int maxDeliveryCount) {
        this.objectMapper = objectMapper;
        this.handler = handler;
        this.deploymentId = deploymentId;
        this.serviceId = serviceId;
        this.maxDeliveryCount = maxDeliveryCount;
    }

    public void onMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        String messageId = message.getMessageId();
        Object messageDeploymentId = message.getApplicationProperties().get(HMCTS_DEPLOYMENT_ID);
        log.info("Received HMC message {} on attempt {} for service {} and deployment {}", messageId,
                 attempt(message), message.getApplicationProperties().get(HMCTS_SERVICE_ID), messageDeploymentId);

        if (!isForThisDeployment(messageDeploymentId)) {
            log.warn("HMC message {} addressed to deployment {} does not match this deployment {}", messageId,
                     messageDeploymentId, deploymentId);
            deadLetter(context, messageId, deadLetterOptions(
                DEPLOYMENT_MISMATCH_REASON,
                "Message deployment " + messageDeploymentId + " does not match " + deploymentId));
            return;
        }

        HmcHearingResponse notification;
        try {
            notification = objectMapper.readValue(message.getBody().toBytes(), HmcHearingResponse.class);
        } catch (IOException e) {
            log.error("HMC message {} could not be read: {}", messageId, e.getClass().getSimpleName());
            deadLetter(context, messageId, deadLetterOptions(
                UNREADABLE_REASON, "Notification body could not be parsed: " + e.getClass().getSimpleName()));
            return;
        }

        Optional<DeadLetterOptions> rejection = validate(notification);
        if (rejection.isPresent()) {
            log.error("HMC message {} failed validation with reason {}", messageId,
                      rejection.get().getDeadLetterReason());
            deadLetter(context, messageId, rejection.get());
            return;
        }

        log.info("HMC message {} validated for hearing {} and case {}", messageId, notification.getHearingID(),
                 notification.getCaseRef());
        process(context, message, notification);
    }

    public void onError(ServiceBusErrorContext context) {
        log.error("HMC Service Bus error from {} during {}: {}", context.getEntityPath(), context.getErrorSource(),
                  failureReason(context.getException()));
    }

    private void process(ServiceBusReceivedMessageContext context, ServiceBusReceivedMessage message,
                         HmcHearingResponse notification) {
        String messageId = message.getMessageId();
        try {
            handler.handle(notification);
        } catch (FeignException e) {
            if (isPermanent(e)) {
                log.error("HMC rejected retrieval of hearing {} for message {} with status {}",
                          notification.getHearingID(), messageId, e.status());
                deadLetter(context, messageId, deadLetterOptions(HMC_REJECTED_REASON, "HMC responded " + e.status()));
            } else {
                retryLater(context, message, notification, "HMC responded " + e.status());
            }
            return;
        } catch (RuntimeException e) {
            log.error("Unexpected failure retrieving hearing {} for message {}", notification.getHearingID(),
                      messageId, e);
            retryLater(context, message, notification, e.getClass().getSimpleName());
            return;
        }

        complete(context, messageId, notification);
    }

    private void retryLater(ServiceBusReceivedMessageContext context, ServiceBusReceivedMessage message,
                            HmcHearingResponse notification, String failure) {
        String messageId = message.getMessageId();
        if (attempt(message) >= maxDeliveryCount) {
            log.error("Giving up on HMC message {} for hearing {} after {} attempts: {}", messageId,
                      notification.getHearingID(), attempt(message), failure);
            deadLetter(context, messageId, deadLetterOptions(RETRIES_EXHAUSTED_REASON, failure));
            return;
        }
        log.warn("Leaving HMC message {} for hearing {} unsettled for redelivery after lock expiry on attempt {}: {}",
                 messageId, notification.getHearingID(), attempt(message), failure);
    }

    private void complete(ServiceBusReceivedMessageContext context, String messageId,
                          HmcHearingResponse notification) {
        try {
            context.complete();
            log.info("Completed HMC message {} for hearing {}", messageId, notification.getHearingID());
        } catch (RuntimeException e) {
            log.error("Failed to complete HMC message {} for hearing {}: {}", messageId, notification.getHearingID(),
                      failureReason(e));
        }
    }

    private void deadLetter(ServiceBusReceivedMessageContext context, String messageId, DeadLetterOptions options) {
        try {
            context.deadLetter(options);
            log.info("Dead-lettered HMC message {} with reason {}", messageId, options.getDeadLetterReason());
        } catch (RuntimeException e) {
            log.error("Failed to dead-letter HMC message {} with reason {}: {}", messageId,
                      options.getDeadLetterReason(), failureReason(e));
        }
    }

    private Optional<DeadLetterOptions> validate(HmcHearingResponse notification) {
        if (notification == null) {
            return Optional.of(deadLetterOptions(UNREADABLE_REASON, "Notification body is empty"));
        }
        if (!Objects.equals(serviceId, notification.getHmctsServiceCode())) {
            return Optional.of(deadLetterOptions(UNEXPECTED_SERVICE_REASON,
                                                 "Expected service code " + serviceId + " but received "
                                                     + notification.getHmctsServiceCode()));
        }
        if (!hasText(notification.getHearingID()) || !hasText(notification.getCaseRef())) {
            return Optional.of(deadLetterOptions(INVALID_NOTIFICATION_REASON,
                                                 "Notification needs hearingID and caseRef"));
        }
        return Optional.empty();
    }

    private static long attempt(ServiceBusReceivedMessage message) {
        return message.getDeliveryCount() + 1;
    }

    private boolean isForThisDeployment(Object messageDeploymentId) {
        return !hasText(deploymentId) || messageDeploymentId instanceof String text && deploymentId.equals(text);
    }

    private static boolean isPermanent(FeignException exception) {
        int status = exception.status();
        return status >= HttpStatus.BAD_REQUEST.value()
            && status < HttpStatus.INTERNAL_SERVER_ERROR.value()
            && !RETRYABLE_CLIENT_ERRORS.contains(status);
    }

    private static String failureReason(Throwable exception) {
        return exception instanceof ServiceBusException serviceBusException
            ? String.valueOf(serviceBusException.getReason())
            : exception.getClass().getSimpleName();
    }

    private static DeadLetterOptions deadLetterOptions(String reason, String description) {
        return new DeadLetterOptions()
            .setDeadLetterReason(reason)
            .setDeadLetterErrorDescription(description);
    }
}
