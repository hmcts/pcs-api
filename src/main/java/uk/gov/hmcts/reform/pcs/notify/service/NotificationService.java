package uk.gov.hmcts.reform.pcs.notify.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
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
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
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
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.DefendantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.TemplatePersonalisation;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
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
            NotificationClaimType.NO_COUNTER_CLAIM,
            NotificationService::buildBasePersonalisation
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED,
            NotificationClaimType.COUNTER_CLAIM,
            NotificationService::buildBasePersonalisation
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS,
            NotificationClaimType.COUNTER_CLAIM,
            NotificationService::buildCounterclaimPaymentSuccessPersonalisation
        );
    }

    public EmailNotificationResponse sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(
        DefendantResponseEntity defendantResponse
    ) {
        return sendDefendantEmail(
            defendantResponse,
            EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED,
            NotificationClaimType.COUNTER_CLAIM,
            NotificationService::buildBasePersonalisation
        );
    }

    public EmailNotificationResponse sendClaimantDraftSavedForLater(long caseReference, PCSCase pcsCase) {
        return sendClaimantEmail(
            caseReference,
            pcsCase,
            EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER,
            NotificationClaimType.POSSESSION_CLAIM,
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
                case SUBMITTED -> notification.setSubmittedAt(now);
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

    protected static TemplatePersonalisation buildBasePersonalisation(long caseReference, PCSCase pcsCase) {
        String toLineClaimantName = getClaimantName(pcsCase.getClaimantInformation());
        String claimantNameUpper = toLineClaimantName.toUpperCase(Locale.ROOT);

        DefendantDetails primaryDefendantDetails = pcsCase.getDefendant1();

        boolean isNameKnown = primaryDefendantDetails.getNameKnown() != null
            && primaryDefendantDetails.getNameKnown().toBoolean();
        String firstName = primaryDefendantDetails.getFirstName();
        String lastName = primaryDefendantDetails.getLastName();

        String primaryDefendantName = isNameKnown && firstName != null && lastName != null
            ? formatNameUpperForNotification(firstName, lastName)
            : "PERSONS UNKNOWN";

        return ClaimantBasePersonalisation.builder()
            .toLineClaimantName(toLineClaimantName)
            .caseNumber(Long.toString(caseReference))
            .claimantName(claimantNameUpper)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    protected static DefendantBasePersonalisation buildBasePersonalisation(DefendantResponseEntity defendantResponse) {
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

        String claimantName = claimant.getOrgName() != null
            ? claimant.getOrgName().toUpperCase(Locale.ROOT)
            : formatNameUpperForNotification(claimant.getFirstName(), claimant.getLastName());
        String primaryDefendantName = formatNameUpperForNotification(defendant.getFirstName(), defendant.getLastName());

        return DefendantBasePersonalisation.builder()
            .firstName(defendant.getFirstName())
            .lastName(defendant.getLastName())
            .caseNumber(formatCaseReference(defendantResponse.getPcsCase().getCaseReference().toString()))
            .claimantName(claimantName)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    protected static TemplatePersonalisation buildCounterclaimPaymentSuccessPersonalisation(
        DefendantResponseEntity defendantResponse) {

        FeePaymentEntity defendantFeePayment = defendantResponse.getClaim().getFeePayment();
        if (defendantFeePayment == null || !defendantFeePayment.getPaymentStatus().equals(PaymentStatus.PAID)) {
            throw new FeePaymentNotFoundException(
                "Paid fee payment not found for defendant response: " + defendantResponse.getId());
        }

        return CounterclaimPaymentSuccessPersonalisation.builder()
            .base(buildBasePersonalisation(defendantResponse))
            .paymentReferenceNumber(defendantFeePayment.getExternalReference())
            .build();
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

    private EmailNotificationResponse sendDefendantEmail(
        DefendantResponseEntity defendantResponse,
        EmailTemplate template,
        NotificationClaimType claimType,
        Function<DefendantResponseEntity, TemplatePersonalisation> personalisationBuilder
    ) {
        if (!partyService.canSendEmailNotification(defendantResponse.getParty())) {
            log.info("Skipping email notification to user: {}", defendantResponse.getParty().getId());
            return null;
        }

        PartyEntity recipientParty = defendantResponse.getParty();
        return scheduleEmailNotification(
            buildRequest(
                templateConfiguration.getTemplateId(template),
                recipientParty.getEmailAddress(),
                claimType,
                personalisationBuilder.apply(defendantResponse)
            ),
            defendantResponse.getPcsCase(),
            defendantResponse.getClaim(),
            recipientParty
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

    private static String formatCaseReference(String caseReference) {
        if (caseReference == null) {
            return null;
        }

        return caseReference.replaceAll("(.{4})(?!$)", "$1-");
    }

    private static String getClaimantEmailAddress(ClaimantContactPreferences claimantContactPreferences) {
        VerticalYesNo isCorrectClaimantContactEmail = claimantContactPreferences.getIsCorrectClaimantContactEmail();
        return isCorrectClaimantContactEmail == null || isCorrectClaimantContactEmail.toBoolean()
            ? claimantContactPreferences.getClaimantContactEmail()
            : claimantContactPreferences.getOverriddenClaimantContactEmail();
    }

    private static String getClaimantName(ClaimantInformation claimantInformation) {
        VerticalYesNo isClaimantNameOverridden = claimantInformation.getIsClaimantNameCorrect();
        return isClaimantNameOverridden == null || isClaimantNameOverridden.toBoolean()
            ? claimantInformation.getClaimantName()
            : claimantInformation.getOverriddenClaimantName();
    }

    private static String formatNameUpperForNotification(String firstName, String lastName) {
        return String.format("%s %s", firstName, lastName).toUpperCase(Locale.ROOT);
    }
}
