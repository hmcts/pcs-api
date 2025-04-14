package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    private  final NotificationClient notificationClient;

    public NotificationService(
        NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public SendEmailResponse sendEmail(EmailNotificationRequest emailRequest, String referenceId) {
        final SendEmailResponse sendEmailResponse;
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();

        try {
            sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

            // job.enqueue

            log.debug("Email sent successfully. Reference ID: {}", referenceId);

            return sendEmailResponse;
        } catch (NotificationClientException notificationClientException) {
            log.error("Failed to send email. Reference ID: {}. Reason: {}",
                referenceId,
                notificationClientException.getMessage(),
                notificationClientException
            );

            throw new NotificationException("Email failed to send, please try again.", notificationClientException);
        }
    }

    public void getNotificationStatus(String referenceId) {

        try {
            Notification emailResponse = notificationClient.getNotificationById(referenceId);
            System.out.println(emailResponse.getStatus());
        } catch (NotificationClientException notificationClientException) {
            log.error("Failed to retrieve email. Reference ID: {}. Reason: {}",
                referenceId,
                notificationClientException.getMessage(),
                notificationClientException
            );

            throw new NotificationException("Email failed to send, please try again.", notificationClientException);
        }
    }
}
