package uk.gov.hmcts.reform.pcs.notify.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.config.NotificationTemplateConfiguration;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SchedulerClient schedulerClient;
    private final NotificationTemplateConfiguration templateConfiguration;
    private final PartyService partyService;

    private static final String NO_CLAIMANT_PARTY_FOUND_MSG = "No claimant party found for defendant response: %s";

    public EmailNotificationResponse sendDefendantResponseNoCounterclaimEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.RESPONSE_NO_COUNTERCLAIM,
            NotificationService::buildBasePersonalisation
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED,
            NotificationService::buildBasePersonalisation
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS,
            NotificationService::buildCounterclaimPaymentSuccessPersonalisation
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED,
            NotificationService::buildBasePersonalisation
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

    protected static Map<String, Object> buildBasePersonalisation(DefendantResponseEntity defendantResponse) {
        PartyEntity defendant = defendantResponse.getParty();

        PartyEntity claimant = defendantResponse.getClaim().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole().equals(PartyRole.CLAIMANT))
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElseThrow(
                () -> new PartyNotFoundException(
                    String.format(NO_CLAIMANT_PARTY_FOUND_MSG, defendantResponse.getId())
                )
            );

        String claimantName = (claimant.getOrgName() != null
            ? claimant.getOrgName()
            : String.format("%s %s", claimant.getFirstName(), claimant.getLastName()))
            .toUpperCase(Locale.ROOT);
        String primaryDefendantName = String.format("%s %s", defendant.getFirstName(), defendant.getLastName())
            .toUpperCase(Locale.ROOT);

        return Map.of(
            "firstName", defendant.getFirstName(),
            "lastName", defendant.getLastName(),
            "caseNumber", formatCaseReference(defendantResponse.getPcsCase().getCaseReference().toString()),
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName
        );
    }

    protected static Map<String, Object> buildCounterclaimPaymentSuccessPersonalisation(
        DefendantResponseEntity defendantResponse) {

        Map<String, Object> base = new HashMap<>(buildBasePersonalisation(defendantResponse));

        FeePaymentEntity defendantFeePayment = defendantResponse.getClaim().getFeePayment();
        if (defendantFeePayment == null || !defendantFeePayment.getPaymentStatus().equals(PaymentStatus.PAID)) {
            throw new FeePaymentNotFoundException(
                "Paid fee payment not found for defendant response: " + defendantResponse.getId());
        }

        base.put("paymentReferenceNumber", defendantFeePayment.getExternalReference());
        return base;
    }

    private EmailNotificationResponse sendDefendantEmail(
        DefendantResponseEntity defendantResponse,
        EmailTemplate template,
        Function<DefendantResponseEntity, Map<String, Object>> personalisationBuilder
    ) {
        if (!partyService.canSendEmailNotification(defendantResponse.getParty())) {
            log.info("Skipping email notification to user: {}", defendantResponse.getParty().getId());
            return null;
        }

        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(template),
                defendantResponse.getParty().getEmailAddress(),
                personalisationBuilder.apply(defendantResponse)
            ),
            defendantResponse.getPcsCase().getId()
        );
    }

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

    private static String formatCaseReference(String caseReference) {
        if (caseReference == null) {
            return null;
        }

        return caseReference.replaceAll("(.{4})(?!$)", "$1-");
    }
}
