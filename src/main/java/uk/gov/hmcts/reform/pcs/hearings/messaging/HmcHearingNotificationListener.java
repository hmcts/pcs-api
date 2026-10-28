package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
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
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcHearingNotificationListener {

    static final String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";
    static final String UNREADABLE_REASON = "UnreadableHmcNotification";
    static final String INVALID_NOTIFICATION_REASON = "InvalidHmcNotification";
    static final String UNEXPECTED_SERVICE_REASON = "UnexpectedHmctsServiceCode";
    static final String HMC_REJECTED_REASON = "HmcRejectedRequest";

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

        if (notification == null) {
            log.error("Dead-lettering empty HMC notification {}", message.getMessageId());
            context.deadLetter(deadLetterOptions(UNREADABLE_REASON, "Notification body is empty"));
            return;
        }

        if (!Objects.equals(serviceId, notification.getHmctsServiceCode())) {
            log.error("Dead-lettering HMC notification {} for service code {}", message.getMessageId(),
                      notification.getHmctsServiceCode());
            context.deadLetter(deadLetterOptions(UNEXPECTED_SERVICE_REASON,
                                                 "Expected service code " + serviceId));
            return;
        }

        if (!hasText(notification.getHearingID()) || !hasText(notification.getCaseRef())) {
            log.error("Dead-lettering HMC notification {} without a hearing id or case reference",
                      message.getMessageId());
            context.deadLetter(deadLetterOptions(INVALID_NOTIFICATION_REASON,
                                                 "Notification needs hearingID and caseRef"));
            return;
        }

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

    public void onError(ServiceBusErrorContext context) {
        log.error("HMC Service Bus error from {} during {}", context.getEntityPath(), context.getErrorSource(),
                  context.getException());
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
