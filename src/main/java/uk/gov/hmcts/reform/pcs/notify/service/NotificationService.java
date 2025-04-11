package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final NotificationClient notificationClient;
    private final NotificationRepository notificationRepository;

    public NotificationService(
        NotificationClient notificationClient,
        NotificationRepository notificationRepository) {
        this.notificationClient = notificationClient;
        this.notificationRepository = notificationRepository;
    }

    public SendEmailResponse sendEmail(EmailNotificationRequest emailRequest) {
        final SendEmailResponse sendEmailResponse;
        final String destinationAddress = emailRequest.getEmailAddress();
        final String templateId = emailRequest.getTemplateId();
        final Map<String, Object> personalisation = emailRequest.getPersonalisation();
        final String referenceId = UUID.randomUUID().toString();

        createCaseNotification(emailRequest.getEmailAddress(), "Email", UUID.randomUUID());
        try {
            sendEmailResponse = notificationClient.sendEmail(
                templateId,
                destinationAddress,
                personalisation,
                referenceId
            );

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

    public CaseNotification createCaseNotification(String recipient, String type, UUID caseId) {

        if (recipient == null || type == null) {
            throw new IllegalArgumentException("Recipient or type cannot be null");
        }
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }

        CaseNotification toSaveCaseNotification = new CaseNotification();
        toSaveCaseNotification.setCaseId(caseId);
        toSaveCaseNotification.setStatus("Schedule Pending");
        toSaveCaseNotification.setType(type);
        toSaveCaseNotification.setRecipient(recipient);

        try {
            CaseNotification savedCaseNotification = notificationRepository.save(toSaveCaseNotification);
            log.info(
                "Case Notification with ID {} has been saved to the database",
                savedCaseNotification.getNotificationId()
            );

            return savedCaseNotification;
        } catch (DataAccessException dataAccessException) {
            log.error(
                "Failed to save Case Notification with Case ID: {}. Reason: {}",
                toSaveCaseNotification.getCaseId(),
                dataAccessException.getMessage(),
                dataAccessException
            );
            throw new NotificationException("Failed to save Case Notification.", dataAccessException);
        }
    }
}
