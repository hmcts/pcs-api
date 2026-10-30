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

    private static final String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";
    private static final String UNREADABLE_REASON = "UnreadableHmcNotification";
    private static final String INVALID_NOTIFICATION_REASON = "InvalidHmcNotification";
    private static final String UNEXPECTED_SERVICE_REASON = "UnexpectedHmctsServiceCode";
    private static final String HMC_REJECTED_REASON = "HmcRejectedRequest";

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

    public HmcHearingNotificationListener(ObjectMapper objectMapper,
                                          HmcHearingNotificationHandler handler,
                                          @Value("${hmc.deployment-id}") String deploymentId,
                                          @Value("${hmc.serviceId}") String serviceId) {
        this.objectMapper = objectMapper;
        this.handler = handler;
        this.deploymentId = deploymentId;
        this.serviceId = serviceId;
    }

    public void onMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Received HMC notification {} with deployment id {}", message.getMessageId(),
                 message.getApplicationProperties().get(HMCTS_DEPLOYMENT_ID));

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

        Optional<DeadLetterOptions> rejection = validate(notification);
        if (rejection.isPresent()) {
            log.error("Dead-lettering HMC notification {} with reason {}", message.getMessageId(),
                      rejection.get().getDeadLetterReason());
            context.deadLetter(rejection.get());
            return;
        }

        process(context, message, notification);
    }

    public void onError(ServiceBusErrorContext context) {
        log.error("HMC Service Bus error from {} during {}: {}", context.getEntityPath(), context.getErrorSource(),
                  failureReason(context.getException()));
    }

    private void process(ServiceBusReceivedMessageContext context, ServiceBusReceivedMessage message,
                         HmcHearingResponse notification) {
        try {
            handler.handle(notification);
        } catch (FeignException e) {
            if (isPermanent(e)) {
                log.error("Dead-lettering HMC notification {} for hearing {} rejected by HMC with status {}",
                          message.getMessageId(), notification.getHearingID(), e.status(), e);
                context.deadLetter(deadLetterOptions(HMC_REJECTED_REASON, "HMC responded " + e.status()));
            } else {
                leaveForRedelivery(message, notification, e);
            }
            return;
        } catch (RuntimeException e) {
            leaveForRedelivery(message, notification, e);
            return;
        }

        context.complete();
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

    private static String failureReason(Throwable exception) {
        return exception instanceof ServiceBusException serviceBusException
            ? String.valueOf(serviceBusException.getReason())
            : exception.getClass().getSimpleName();
    }

    private void leaveForRedelivery(ServiceBusReceivedMessage message, HmcHearingResponse notification,
                                    RuntimeException cause) {
        log.error("Leaving HMC notification {} for hearing {} unsettled for redelivery after lock expiry",
                  message.getMessageId(), notification.getHearingID(), cause);
    }

    private boolean isPermanent(FeignException exception) {
        int status = exception.status();
        return status >= HttpStatus.BAD_REQUEST.value()
            && status < HttpStatus.INTERNAL_SERVER_ERROR.value()
            && !RETRYABLE_CLIENT_ERRORS.contains(status);
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
