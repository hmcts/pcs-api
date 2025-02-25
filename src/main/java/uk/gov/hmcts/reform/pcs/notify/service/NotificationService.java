package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.SendEmail;
import uk.gov.service.notify.NotificationClient;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
public class NotificationService {

    private  final NotificationClient notificationClient;
    private final AuthTokenGenerator authTokenGenerator;

    public NotificationService(
        @Value("${notify.api-key}") String apiKey,
        AuthTokenGenerator authTokenGenerator) {
        this.notificationClient = new NotificationClient(apiKey);
        this.authTokenGenerator = authTokenGenerator;
    }

    public NotificationResponse sendEmail(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestBody SendEmail emailRequest) {
        final SendEmailResponse sendEmailResponse;
        final String serviceAuthorisation = authTokenGenerator.generate();
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();
        final String referenceId = UUID.randomUUID().toString();

        try {
            sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            log.debug("Email sent successfully. Reference ID: {}", referenceId);

            return getNotificationResponse(sendEmailResponse);
        } catch (NotificationClientException notificationClientException) {
            log.error("Failed to send email. Reference ID: {}. Reason: {}",
                      referenceId,
                      notificationClientException.getMessage(),
                      notificationClientException
            );

            throw new NotificationException(notificationClientException);
        }
    }

    private static NotificationResponse getNotificationResponse(SendEmailResponse sendEmailResponse) {
        return NotificationResponse.builder()
            .notificationId(sendEmailResponse.getNotificationId())
            .reference(sendEmailResponse.getReference())
            .build();
    }
}
