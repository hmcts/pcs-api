package uk.gov.hmcts.reform.pcs.notify.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.config.NotificationTemplateConfiguration;
import uk.gov.hmcts.reform.pcs.notify.task.SendEmailTaskComponent;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SchedulerClient schedulerClient;
    private final NotificationTemplateConfiguration templateConfiguration;

    public NotificationService(
        NotificationRepository notificationRepository,
        SchedulerClient schedulerClient,
        NotificationTemplateConfiguration templateConfiguration) {
        this.notificationRepository = notificationRepository;
        this.schedulerClient = schedulerClient;
        this.templateConfiguration = templateConfiguration;
    }

    /**
     * Schedules a defendant response no counterclaim email notification
     *
     * @param to the party you are sending the email notification to
     * @param pcsCase the associated case for the notification
     * @return an instance of EmailNotificationResponse containing the task ID, notification status,
     *         and notification ID associated with the scheduled email notification
     */
    public EmailNotificationResponse sendDefendantResponseNoCounterclaimEmailNotification(
        PartyEntity to,
        PcsCaseEntity pcsCase
    ) {
        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(EmailTemplate.RESPONSE_NO_COUNTERCLAIM),
                to.getEmailAddress(),
                buildBasePersonalisation(to, pcsCase)
            ),
            pcsCase.getId()
        );
    }

    /**
     * Schedules a defendant response counterclaim payment required email notification
     *
     * @param to the party you are sending the email notification to
     * @param pcsCase the associated case for the notification
     * @return an instance of EmailNotificationResponse containing the task ID, notification status,
     *         and notification ID associated with the scheduled email notification
     */
    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(
        PartyEntity to,
        PcsCaseEntity pcsCase
    ) {
        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED),
                to.getEmailAddress(),
                buildBasePersonalisation(to, pcsCase)
            ),
            pcsCase.getId()
        );
    }

    /**
     * Schedules a defendant response counterclaim payment success email notification
     *
     * @param to the party you are sending the email notification to
     * @param pcsCase the associated case for the notification
     * @return an instance of EmailNotificationResponse containing the task ID, notification status,
     *         and notification ID associated with the scheduled email notification
     */
    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(
        PartyEntity to,
        PcsCaseEntity pcsCase,
        PaymentAgreementEntity paymentAgreement
    ) {
        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS),
                to.getEmailAddress(),
                buildCounterclaimPaymentSuccessPersonalisation(to, pcsCase, paymentAgreement)
            ),
            pcsCase.getId()
        );
    }

    /**
     * Schedules a defendant response counterclaim no payment required email notification
     *
     * @param to the party you are sending the email notification to
     * @param pcsCase the associated case for the notification
     * @return an instance of EmailNotificationResponse containing the task ID, notification status,
     *         and notification ID associated with the scheduled email notification
     */
    public EmailNotificationResponse sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(
        PartyEntity to,
        PcsCaseEntity pcsCase
    ) {
        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED),
                to.getEmailAddress(),
                buildBasePersonalisation(to, pcsCase)
            ),
            pcsCase.getId()
        );
    }

    /**
     * Schedules an email notification to be sent based on the provided request data.
     * The method generates a unique task ID and notification ID, creates the necessary
     * state, and schedules the email sending task. If the task is already scheduled,
     * it prevents duplicate scheduling.
     *
     * @param emailRequest the request object containing details needed to send the email,
     *                     such as email address, template ID, personalisation details,
     *                     reference, and email reply-to ID
     * @param caseId the id for the associated case
     * @return an instance of EmailNotificationResponse containing the task ID, notification status,
     *         and notification ID associated with the scheduled email notification
     */
    public EmailNotificationResponse scheduleEmailNotification(EmailNotificationRequest emailRequest, UUID caseId) {
        String taskId = UUID.randomUUID().toString();

        CaseNotification caseNotification = createCaseNotification(
            emailRequest.getEmailAddress(),
            caseId,
            taskId
        );

        EmailState emailState = new EmailState(
            taskId,
            emailRequest.getEmailAddress(),
            emailRequest.getTemplateId(),
            emailRequest.getPersonalisation(),
            emailRequest.getReference(),
            emailRequest.getEmailReplyToId(),
            null, // notification ID will be set after sending
            caseNotification.getNotificationId()
        );

        boolean scheduled = schedulerClient.scheduleIfNotExists(
            SendEmailTaskComponent.sendEmailTask
                .instance(taskId)
                .data(emailState)
                .scheduledTo(Instant.now())
        );

        if (!scheduled) {
            log.warn("Task with ID {} already exists", taskId);
        }

        updateNotificationStatus(caseNotification, NotificationStatus.SCHEDULED, null);

        EmailNotificationResponse response = new EmailNotificationResponse();
        response.setTaskId(taskId);
        response.setStatus(NotificationStatus.SCHEDULED.toString());
        response.setNotificationId(caseNotification.getNotificationId());

        log.info("Email notification scheduled with task ID: {} and notification ID: {}",
                 taskId, caseNotification.getNotificationId());

        return response;
    }

    /**
     * Updates the status of a notification after it has been sent by associating it
     * with a provider notification identifier and updating its status to SUBMITTED.
     *
     * @param dbNotificationId the unique identifier of the notification stored in the database
     * @param providerNotificationId the unique identifier of the notification assigned by the provider
     */
    public void updateNotificationAfterSending(UUID dbNotificationId, UUID providerNotificationId) {
        Optional<CaseNotification> notificationOpt = notificationRepository.findById(dbNotificationId);
        if (notificationOpt.isEmpty()) {
            log.error("Notification not found with ID: {}", dbNotificationId);
            return;
        }

        CaseNotification notification = notificationOpt.get();
        updateNotificationStatus(notification, NotificationStatus.SUBMITTED, providerNotificationId);
    }

    /**
     * Updates the notification status to indicate a permanent failure after an email sending operation fails.
     * Logs the error details associated with the failure.
     *
     * @param dbNotificationId the unique identifier of the notification in the database
     * @param exception the exception containing the details of the failure
     */
    public void updateNotificationAfterFailure(UUID dbNotificationId, Exception exception) {
        Optional<CaseNotification> notificationOpt = notificationRepository.findById(dbNotificationId);
        if (notificationOpt.isEmpty()) {
            log.error("Notification not found with ID on failure: {}", dbNotificationId);
            return;
        }

        CaseNotification notification = notificationOpt.get();
        updateNotificationStatus(notification, NotificationStatus.PERMANENT_FAILURE, null);
        log.error("Email sending failed for notification ID: {}, error: {}",
                    dbNotificationId, exception.getMessage());
    }

    /**
     * Creates a new CaseNotification, sets its properties, saves it to the database,
     * and returns the saved notification.
     * Throws a NotificationException if the save operation fails.
     *
     * @param recipient the recipient email address for the notification
     * @param caseId the unique identifier of the case associated with the notification
     * @param taskId the identifier of the associated task for logging purposes
     * @return the saved CaseNotification object
     * @throws NotificationException if an error occurs while saving the notification
     */
    private CaseNotification createCaseNotification(String recipient, UUID caseId, String taskId) {
        CaseNotification toSaveNotification = new CaseNotification();
        toSaveNotification.setCaseId(caseId);
        toSaveNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);
        toSaveNotification.setType("Email");
        toSaveNotification.setRecipient(recipient);

        try {
            CaseNotification savedNotification = notificationRepository.save(toSaveNotification);
            log.info(
                "Case Notification with ID {} has been saved to the database with task ID {}",
                savedNotification.getNotificationId(), taskId
            );
            return savedNotification;
        } catch (DataAccessException dataAccessException) {
            log.error(
                "Failed to save Case Notification with Case ID: {}. Reason: {}",
                toSaveNotification.getCaseId(),
                dataAccessException.getMessage(),
                dataAccessException
            );
            throw new NotificationException("Failed to save Case Notification.", dataAccessException);
        }
    }

    /**
     * Updates the status of a notification based on the provided notification ID and status string.
     * If the notification ID is not found, an error is logged, and the process terminates.
     * If the provided status string does not match a valid notification status, a warning is logged.
     *
     * @param dbNotificationId the unique identifier of the notification in the database
     * @param statusString the new status to set for the notification, represented as a string
     */
    public void updateNotificationStatus(UUID dbNotificationId, String statusString) {
        Optional<CaseNotification> notificationOpt = notificationRepository.findById(dbNotificationId);
        if (notificationOpt.isEmpty()) {
            log.error("Notification not found with ID on status update: {}", dbNotificationId);
            return;
        }

        CaseNotification notification = notificationOpt.get();
        try {
            NotificationStatus status = NotificationStatus.fromString(statusString);
            updateNotificationStatus(notification, status, null);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification status: {}", statusString);
        }
    }

    /**
     * Updates the status of the given notification, along with relevant timestamps
     * and the provider notification ID if applicable. This method saves the updated
     * notification details to the repository.
     *
     * @param notification The notification object whose status is to be updated.
     * @param status The new status to be applied to the notification.
     * @param providerNotificationId The unique identifier of the notification from
     *        the provider, if available.
     */
    private void updateNotificationStatus(
        CaseNotification notification,
        NotificationStatus status,
        UUID providerNotificationId) {

        try {
            notification.setStatus(status);
            notification.setLastUpdatedAt(Instant.now());

            if (providerNotificationId != null) {
                notification.setProviderNotificationId(providerNotificationId);
            }

            if (status == NotificationStatus.SENDING) {
                notification.setSubmittedAt(Instant.now());
            }

            notificationRepository.save(notification);
            log.info("Updated notification status to {} for notification ID: {}",
                        status, notification.getNotificationId());
        } catch (Exception e) {
            log.error("Error updating notification status to {}: {}",
                        status, e.getMessage(), e);
        }
    }

    /**
     * Creates a personalization map for a basic email template.
     * A basic email template has the following personalizations:
     *  - firstName: the first name of the email recipient
     *  - lastName: the last name of the email recipient
     *  - caseNumber: the case number for the associated case
     *  - claimantName: the name of the claimant in the associated case
     *  - primaryDefendantName: the name of the primary defendant in the associated case
     *
     * @param to the party you are sending the email notification to
     * @param pcsCase the associated case for the notification
     * @return a personalization map for the template
     */
    protected static Map<String, Object> buildBasePersonalisation(PartyEntity to, PcsCaseEntity pcsCase) {

        return Map.of(
            "firstName", to.getFirstName(),
            "lastName", to.getLastName(),
            "caseNumber", pcsCase.getCaseReference(),
            "claimantName", "",
            "primaryDefendantName", ""
        );
    }

    /**
     * Creates a personalization map for a counterclaim payment success email template.
     * A basic email template has the following personalizations:
     *  - firstName: the first name of the email recipient
     *  - lastName: the last name of the email recipient
     *  - caseNumber: the case number for the associated case
     *  - claimantName: the name of the claimant in the associated case
     *  - primaryDefendantName: the name of the primary defendant in the associated case
     *  - paymentReferenceNumber: the associated payment reference number
     *
     * @param to the party you are sending the email notification to
     * @param pcsCase the associated case for the notification
     * @return a personalization map for the template
     */
    protected static Map<String, Object> buildCounterclaimPaymentSuccessPersonalisation(
        PartyEntity to, PcsCaseEntity pcsCase, PaymentAgreementEntity paymentAgreement) {

        Map<String, Object> base = new HashMap<>(buildBasePersonalisation(to, pcsCase));
        // todo change this
        base.put("paymentReferenceNumber", paymentAgreement.getId());
        return base;
    }

    /**
     * Builds EmailNotificationRequest object for the corresponding template
     *
     * @param templateId the gov notify template id for the notification you are sending
     * @param email the email address you are sending the notification to
     * @param personalisation the personalization you are inserting into the template
     * @return the EmailNotificationRequest object built from the three inputs
     */
    protected static EmailNotificationRequest buildRequest(
        String templateId,
        String email,
        Map<String, Object> personalisation
    ) {
        return EmailNotificationRequest.builder()
            .templateId(templateId)
            .emailAddress(email)
            .personalisation(personalisation)
            .build();
    }
}
