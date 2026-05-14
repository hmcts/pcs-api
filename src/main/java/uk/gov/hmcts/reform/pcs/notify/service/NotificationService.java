package uk.gov.hmcts.reform.pcs.notify.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.config.NotificationTemplateConfiguration;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationRecipient;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationType;
import uk.gov.hmcts.reform.pcs.notify.task.SendEmailTaskComponent;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.EmailState;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.TemplatePersonalisation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final PartyService partyService;
    private final SchedulerClient schedulerClient;
    private final NotificationTemplateConfiguration templateConfiguration;
    private final NotificationPersonalisationFactory notificationPersonalisationFactory;

    public EmailNotificationResponse sendDefendantResponseNoCounterclaimEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendEmail(
            defendantRecipient(defendantResponse),
            EmailTemplate.RESPONSE_NO_COUNTERCLAIM,
            NotificationClaimType.COUNTER_CLAIM,
            notificationPersonalisationFactory.forDefendant(defendantResponse)
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendEmail(
            defendantRecipient(defendantResponse),
            EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED,
            NotificationClaimType.COUNTER_CLAIM,
            notificationPersonalisationFactory.forDefendant(defendantResponse)
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendEmail(
            defendantRecipient(defendantResponse),
            EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS,
            NotificationClaimType.COUNTER_CLAIM,
            notificationPersonalisationFactory.counterclaimSuccess(defendantResponse)
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendEmail(
            defendantRecipient(defendantResponse),
            EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED,
            NotificationClaimType.COUNTER_CLAIM,
            notificationPersonalisationFactory.forDefendant(defendantResponse)
        );
    }

    public EmailNotificationResponse sendClaimantDraftSavedForLater(long caseReference, PCSCase pcsCase) {
        NotificationRecipient recipient = claimantRecipient(pcsCase);

        if (recipient.email() == null) {
            log.info("Skipping email notification to claimant on case: {}", caseReference);
            return null;
        }

        return sendEmail(
            recipient,
            EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER,
            NotificationClaimType.POSSESSION_CLAIM,
            notificationPersonalisationFactory.forClaimant(caseReference, pcsCase)
        );
    }

    public EmailNotificationResponse sendClaimantDefendantHasMadeCounterclaimEmail(ClaimEntity claim) {
        return sendEmail(
            claimantRecipient(claim),
            EmailTemplate.MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM,
            NotificationClaimType.COUNTER_CLAIM,
            notificationPersonalisationFactory.forClaimant(claim, claim.getClaimantParty())
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
     * @param pcsCase the associated case entity
     * @param claim the associated claim entity
     * @param party the associated party entity
     * @return an instance of EmailNotificationResponse containing the task ID, notification status,
     *         and notification ID associated with the scheduled email notification
     */
    public EmailNotificationResponse scheduleEmailNotification(
        EmailNotificationRequest emailRequest,
        PcsCaseEntity pcsCase,
        ClaimEntity claim,
        PartyEntity party
    ) {
        String taskId = UUID.randomUUID().toString();

        CaseNotification caseNotification = createCaseNotification(
            emailRequest,
            taskId,
            pcsCase,
            claim,
            party
        );

        EmailState emailState = new EmailState(
            taskId,
            emailRequest.getEmailAddress(),
            emailRequest.getTemplateId(),
            emailRequest.getPersonalisation(),
            emailRequest.getReference(),
            emailRequest.getEmailReplyToId(),
            null, // notification ID will be set after sending
            caseNotification.getId()
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
        response.setNotificationId(caseNotification.getId());

        log.info("Email notification scheduled with task ID: {} and notification ID: {}",
                 taskId, caseNotification.getId());

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
     * @param request the email notification request
     * @param taskId the identifier of the associated task for logging purposes
     * @param pcsCase the associated case entity
     * @param claim the associated claim entity
     * @param party the associated party entity
     * @return the saved CaseNotification object
     * @throws NotificationException if an error occurs while saving the notification
     */
    private CaseNotification createCaseNotification(
        EmailNotificationRequest request,
        String taskId,
        PcsCaseEntity pcsCase,
        ClaimEntity claim,
        PartyEntity party
    ) {
        CaseNotification toSaveNotification = new CaseNotification();
        toSaveNotification.setPcsCase(pcsCase);
        toSaveNotification.setClaimId(claim);
        toSaveNotification.setPartyId(party);
        toSaveNotification.setClaimType(request.getClaimType());
        toSaveNotification.setStatus(NotificationStatus.PENDING_SCHEDULE);
        toSaveNotification.setType(NotificationType.EMAIL);
        toSaveNotification.setRecipient(request.getEmailAddress());

        try {
            CaseNotification savedNotification = notificationRepository.save(toSaveNotification);
            log.info(
                "Case Notification with ID {} has been saved to the database with task ID {}",
                savedNotification.getId(), taskId
            );
            return savedNotification;
        } catch (DataAccessException dataAccessException) {
            log.error(
                "Failed to save Case Notification with Case ID: {}. Reason: {}",
                toSaveNotification.getPcsCase(),
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
            Instant now = Instant.now();
            notification.setStatus(status);
            notification.setLastUpdatedAt(now);

            if (providerNotificationId != null) {
                notification.setProviderNotificationId(providerNotificationId);
            }

            switch (status) {
                case SENDING -> notification.setSubmittedAt(now);
                case SCHEDULED -> notification.setScheduledAt(now);
            }

            notificationRepository.save(notification);
            log.info("Updated notification status to {} for notification ID: {}",
                        status, notification.getId());
        } catch (Exception e) {
            log.error("Error updating notification status to {}: {}",
                        status, e.getMessage(), e);
        }
    }

    private EmailNotificationResponse sendEmail(
        NotificationRecipient recipient,
        EmailTemplate template,
        NotificationClaimType claimType,
        TemplatePersonalisation personalisation
    ) {
        if (recipient.party() != null && !partyService.canSendEmailNotification(recipient.party())) {
            log.info("Skipping email notification to user: {}", recipient.party().getId());
            return null;
        }

        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(template),
                recipient.email(),
                claimType,
                personalisation
            ),
            recipient.pcsCase(),
            recipient.claim(),
            recipient.party()
        );
    }

    private EmailNotificationResponse sendClaimantEmail(
        long caseReference,
        PCSCase pcsCase,
        EmailTemplate template,
        NotificationClaimType claimType,
        BiFunction<Long, PCSCase, TemplatePersonalisation> personalisationBuilder
    ) {
        String claimantEmail = getClaimantEmailAddress(pcsCase.getClaimantContactPreferences());
        if (claimantEmail == null) {
            log.info("Skipping email notification to claimant on case: {}", caseReference);
            return null;
        }

        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(template),
                claimantEmail,
                claimType,
                personalisationBuilder.apply(caseReference, pcsCase)
            ),
            null,
            null,
            null
        );
    }

    protected static EmailNotificationRequest buildRequest(
        String templateId,
        String email,
        NotificationClaimType claimType,
        TemplatePersonalisation personalisation
    ) {
        return EmailNotificationRequest.builder()
            .templateId(templateId)
            .emailAddress(email)
            .personalisation(personalisation.toMap())
            .claimType(claimType)
            .build();
    }

    private static String getClaimantEmailAddress(ClaimantContactPreferences claimantContactPreferences) {
        VerticalYesNo isCorrectClaimantContactEmail = claimantContactPreferences.getIsCorrectClaimantContactEmail();
        return isCorrectClaimantContactEmail == null || isCorrectClaimantContactEmail.toBoolean()
            ? claimantContactPreferences.getClaimantContactEmail()
            : claimantContactPreferences.getOverriddenClaimantContactEmail();
    }

    private NotificationRecipient claimantRecipient(PCSCase pcsCase) {
        return new NotificationRecipient(
            getClaimantEmailAddress(pcsCase.getClaimantContactPreferences()),
            null,
            null,
            null
        );
    }

    private NotificationRecipient claimantRecipient(ClaimEntity claim) {
        PartyEntity claimant = claim.getClaimantParty();

        if (claimant == null) {
            throw new PartyNotFoundException("No claimant party found for claim: " + claim.getId());
        }

        return new NotificationRecipient(
            claimant.getEmailAddress(),
            claimant,
            claim.getPcsCase(),
            claim
        );
    }

    private NotificationRecipient defendantRecipient(DefendantResponseEntity response) {
        PartyEntity defendant = response.getParty();

        if (defendant == null) {
            throw new PartyNotFoundException("No defendant party found for response: " + response.getId());
        }

        return new NotificationRecipient(
            defendant.getEmailAddress(),
            defendant,
            response.getPcsCase(),
            response.getClaim()
        );
    }
}
