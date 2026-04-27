package uk.gov.hmcts.reform.pcs.notify.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.config.NotificationTemplateConfiguration;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private NotificationTemplateConfiguration templateConfiguration;

    private NotificationService notificationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEMPLATE_ID = "template-123";
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID PROVIDER_NOTIFICATION_ID = UUID.randomUUID();
    private static final String STATUS_STRING = "delivered";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, schedulerClient, templateConfiguration);
    }

    @Nested
    @DisplayName("Schedule Email Notification Tests")
    class ScheduleEmailNotificationTests {

        @Test
        @DisplayName("Should successfully schedule email notification")
        void shouldSuccessfullyScheduleEmailNotification() {
            EmailNotificationRequest request = createValidEmailRequest();
            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(request, UUID.randomUUID());

            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());
            assertThat(response.getNotificationId()).isEqualTo(savedNotification.getNotificationId());

            verify(notificationRepository, times(2)).save(any(CaseNotification.class));
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should handle when task already exists")
        void shouldHandleWhenTaskAlreadyExists() {
            EmailNotificationRequest request = createValidEmailRequest();
            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(false);

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(request, UUID.randomUUID());

            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(notificationRepository, times(2)).save(any(CaseNotification.class));
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should schedule email with minimal request")
        void shouldScheduleEmailWithMinimalRequest() {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .build();

            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(request, UUID.randomUUID());

            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(notificationRepository, times(2)).save(any(CaseNotification.class));
        }

        @Test
        @DisplayName("Should throw exception when database save fails")
        void shouldThrowExceptionWhenDatabaseSaveFails() {
            EmailNotificationRequest request = createValidEmailRequest();

            when(notificationRepository.save(any(CaseNotification.class)))
                .thenThrow(new DataAccessException("Database error") {});

            assertThatThrownBy(() -> notificationService.scheduleEmailNotification(request, UUID.randomUUID()))
                .isInstanceOf(NotificationException.class)
                .hasMessage("Failed to save Case Notification.");

            verify(notificationRepository).save(any(CaseNotification.class));
        }

        @Test
        @DisplayName("Should create notification with correct initial status")
        void shouldCreateNotificationWithCorrectInitialStatus() {
            EmailNotificationRequest request = createValidEmailRequest();
            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            notificationService.scheduleEmailNotification(request, UUID.randomUUID());

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification firstSave = notificationCaptor.getAllValues().getFirst();
            assertThat(firstSave.getStatus()).isEqualTo(NotificationStatus.PENDING_SCHEDULE);
            assertThat(firstSave.getType()).isEqualTo("Email");
            assertThat(firstSave.getRecipient()).isEqualTo(TEST_EMAIL);

            CaseNotification secondSave = notificationCaptor.getAllValues().get(1);
            assertThat(secondSave.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
        }
    }

    @Nested
    @DisplayName("Update Notification After Sending Tests")
    class UpdateNotificationAfterSendingTests {

        @Test
        @DisplayName("Should successfully update notification after sending")
        void shouldSuccessfullyUpdateNotificationAfterSending() {
            CaseNotification notification = createCaseNotification();

            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(notification);

            notificationService.updateNotificationAfterSending(NOTIFICATION_ID, PROVIDER_NOTIFICATION_ID);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository).save(notification);

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository).save(notificationCaptor.capture());

            CaseNotification updatedNotification = notificationCaptor.getValue();
            assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.SUBMITTED);
            assertThat(updatedNotification.getProviderNotificationId()).isEqualTo(PROVIDER_NOTIFICATION_ID);
        }

        @Test
        @DisplayName("Should handle notification not found")
        void shouldHandleNotificationNotFound() {
            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

            notificationService.updateNotificationAfterSending(NOTIFICATION_ID, PROVIDER_NOTIFICATION_ID);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any(CaseNotification.class));
        }
    }

    @Nested
    @DisplayName("Update Notification After Failure Tests")
    class UpdateNotificationAfterFailureTests {

        @Test
        @DisplayName("Should successfully update notification after failure")
        void shouldSuccessfullyUpdateNotificationAfterFailure() {
            CaseNotification notification = createCaseNotification();
            Exception exception = new RuntimeException("Test error");

            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(notification);

            notificationService.updateNotificationAfterFailure(NOTIFICATION_ID, exception);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository).save(notification);

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository).save(notificationCaptor.capture());

            CaseNotification updatedNotification = notificationCaptor.getValue();
            assertThat(updatedNotification.getStatus()).isEqualTo(NotificationStatus.PERMANENT_FAILURE);
        }

        @Test
        @DisplayName("Should handle notification not found on failure")
        void shouldHandleNotificationNotFoundOnFailure() {
            Exception exception = new RuntimeException("Test error");

            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

            notificationService.updateNotificationAfterFailure(NOTIFICATION_ID, exception);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any(CaseNotification.class));
        }
    }

    @Nested
    @DisplayName("Update Notification Status Tests")
    class UpdateNotificationStatusTests {

        @Test
        @DisplayName("Should successfully update notification status with valid status string")
        void shouldSuccessfullyUpdateNotificationStatusWithValidStatusString() {
            CaseNotification notification = createCaseNotification();

            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(CaseNotification.class))).thenReturn(notification);

            notificationService.updateNotificationStatus(NOTIFICATION_ID, STATUS_STRING);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("Should handle unknown status string")
        void shouldHandleUnknownStatusString() {
            CaseNotification notification = createCaseNotification();

            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

            notificationService.updateNotificationStatus(NOTIFICATION_ID, "unknown-status");

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any(CaseNotification.class));
        }

        @Test
        @DisplayName("Should handle notification not found on status update")
        void shouldHandleNotificationNotFoundOnStatusUpdate() {
            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

            notificationService.updateNotificationStatus(NOTIFICATION_ID, STATUS_STRING);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any(CaseNotification.class));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create service with dependencies")
        void shouldCreateServiceWithDependencies() {
            NotificationService service = new NotificationService(notificationRepository, schedulerClient, templateConfiguration);

            assertThat(service).isNotNull();
        }
    }

    @Test
    @DisplayName("Should set submittedAt when status is SENDING")
    void shouldSetSubmittedAtWhenStatusIsSending() {
        CaseNotification notification = createCaseNotification();
        notification.setSubmittedAt(null);

        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(notification);

        notificationService.updateNotificationStatus(NOTIFICATION_ID, "SENDING");

        verify(notificationRepository).findById(NOTIFICATION_ID);

        ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        CaseNotification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getSubmittedAt()).isNotNull();
        assertThat(savedNotification.getSubmittedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle exception when saving notification status update")
    void shouldHandleExceptionWhenSavingNotificationStatusUpdate() {
        CaseNotification notification = createCaseNotification();

        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(CaseNotification.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatCode(() -> notificationService.updateNotificationStatus(NOTIFICATION_ID, STATUS_STRING))
            .doesNotThrowAnyException();

        verify(notificationRepository).findById(NOTIFICATION_ID);
        verify(notificationRepository).save(any(CaseNotification.class));
    }

    @Nested
    @DisplayName("Wrapper Email Notification Methods Tests")
    class WrapperEmailNotificationTests {

        private DefendantResponseEntity defendantResponse;

        @BeforeEach
        void setUp() {
            PartyEntity party = new PartyEntity();
            party.setEmailAddress(TEST_EMAIL);
            party.setFirstName("John");
            party.setLastName("Doe");

            PcsCaseEntity pcsCase = new PcsCaseEntity();
            pcsCase.setId(UUID.randomUUID());
            pcsCase.setCaseReference(1234567890L);

            PaymentAgreementEntity paymentAgreement = new PaymentAgreementEntity();
            paymentAgreement.setId(UUID.randomUUID());

            defendantResponse = new DefendantResponseEntity();
            defendantResponse.setParty(party);
            defendantResponse.setPcsCase(pcsCase);
            defendantResponse.setPaymentAgreement(paymentAgreement);

            ClaimEntity claim = new ClaimEntity();
            PartyEntity claimantParty = new PartyEntity();
            claimantParty.setFirstName("Jane");
            claimantParty.setLastName("Smith");
            ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
                .party(claimantParty)
                .role(PartyRole.CLAIMANT)
                .build();
            claim.setClaimParties(new java.util.ArrayList<>(java.util.List.of(claimParty)));
            claim.setFeePayments(new java.util.ArrayList<>());
            defendantResponse.setClaim(claim);
        }

        @Test
        @DisplayName("Should send defendant response no counterclaim email")
        void shouldSendDefendantResponseNoCounterclaimEmail() {
            when(templateConfiguration.getTemplateId(EmailTemplate.RESPONSE_NO_COUNTERCLAIM))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(templateConfiguration).getTemplateId(EmailTemplate.RESPONSE_NO_COUNTERCLAIM);

            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should send counterclaim payment required email")
        void shouldSendCounterclaimPaymentRequiredEmail() {
            when(templateConfiguration.getTemplateId(
                EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse);

            assertThat(response).isNotNull();

            verify(templateConfiguration)
                .getTemplateId(EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED);
        }

        @Test
        @DisplayName("Should send counterclaim payment success email")
        void shouldSendCounterclaimPaymentSuccessEmail() {
            when(templateConfiguration.getTemplateId(
                EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS))
                .thenReturn(TEMPLATE_ID);

            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-123")
                .build();
            defendantResponse.getClaim().setFeePayments(java.util.List.of(feePayment));

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(templateConfiguration)
                .getTemplateId(EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS);

            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should send counterclaim no payment required email")
        void shouldSendCounterclaimNoPaymentRequiredEmail() {
            when(templateConfiguration.getTemplateId(
                EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(templateConfiguration)
                .getTemplateId(EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED);

            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }
    }

    @Nested
    @DisplayName("buildBasePersonalisation")
    class BuildBasePersonalisationTests {
        @Test
        @DisplayName("Should build correct base personalisation")
        void shouldBuildBasePersonalisation() {
            Map<String, Object> result =
                NotificationService.buildBasePersonalisation(createDefendantResponse());

            assertThat(result)
                .hasSize(5)
                .containsEntry("firstName", "John")
                .containsEntry("lastName", "Doe")
                .containsEntry("caseNumber", "1234567890")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }

        @Test
        @DisplayName("Should throw PartyNotFoundException when no claimant found")
        void shouldThrowExceptionWhenNoClaimantFound() {
            DefendantResponseEntity response = createDefendantResponse();
            response.getClaim().getClaimParties().clear();

            assertThatThrownBy(() -> NotificationService.buildBasePersonalisation(response))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessageContaining("No claimant party found");
        }

        @Test
        @DisplayName("Should include base fields and paymentReferenceNumber")
        void shouldIncludePaymentReferenceNumber() {
            DefendantResponseEntity response = createDefendantResponse();
            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-123")
                .build();
            response.getClaim().setFeePayments(java.util.List.of(feePayment));

            Map<String, Object> result =
                NotificationService.buildCounterclaimPaymentSuccessPersonalisation(response);

            assertThat(result)
                .containsKey("paymentReferenceNumber")
                .containsEntry("paymentReferenceNumber", "PAY-123")
                .containsEntry("firstName", "John")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE")
                .hasSize(6);
        }

        @Test
        @DisplayName("Should throw FeePaymentNotFoundException when no paid fee payment found")
        void shouldThrowExceptionWhenNoPaidFeePaymentFound() {
            DefendantResponseEntity response = createDefendantResponse();
            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.NOT_PAID)
                .externalReference("PAY-123")
                .build();
            response.getClaim().setFeePayments(java.util.List.of(feePayment));

            assertThatThrownBy(() -> NotificationService.buildCounterclaimPaymentSuccessPersonalisation(response))
                .isInstanceOf(FeePaymentNotFoundException.class)
                .hasMessageContaining("Paid fee payment not found");
        }

        @Test
        @DisplayName("Should build request with all fields")
        void shouldBuildRequest() {
            Map<String, Object> personalisation = Map.of("key", "value");

            EmailNotificationRequest request =
                NotificationService.buildRequest("template-1", "test@example.com", personalisation);

            assertThat(request.getTemplateId()).isEqualTo("template-1");
            assertThat(request.getEmailAddress()).isEqualTo("test@example.com");
            assertThat(request.getPersonalisation()).isEqualTo(personalisation);
        }

        @Test
        @DisplayName("Should allow null personalisation")
        void shouldAllowNullPersonalisation() {
            EmailNotificationRequest request =
                NotificationService.buildRequest("template-1", "test@example.com", null);

            assertThat(request.getPersonalisation()).isNull();
        }

        @Test
        @DisplayName("Should allow empty personalisation")
        void shouldAllowEmptyPersonalisation() {
            EmailNotificationRequest request =
                NotificationService.buildRequest("template-1", "test@example.com", Map.of());

            assertThat(request.getPersonalisation()).isEmpty();
        }
    }

    private EmailNotificationRequest createValidEmailRequest() {
        Map<String, Object> personalisation = new HashMap<>();
        personalisation.put("name", "Test User");
        personalisation.put("reference", "TEST-REF-123");

        return EmailNotificationRequest.builder()
            .emailAddress(TEST_EMAIL)
            .templateId(TEMPLATE_ID)
            .personalisation(personalisation)
            .reference("external-ref-456")
            .emailReplyToId("reply-to-789")
            .build();
    }

    private CaseNotification createCaseNotification() {
        CaseNotification notification = new CaseNotification();
        notification.setNotificationId(NOTIFICATION_ID);
        notification.setCaseId(UUID.randomUUID());
        notification.setRecipient(TEST_EMAIL);
        notification.setType("Email");
        notification.setStatus(NotificationStatus.PENDING_SCHEDULE);
        return notification;
    }

    private PartyEntity createParty(String firstName, String lastName, String email) {
        PartyEntity party = new PartyEntity();
        party.setFirstName(firstName);
        party.setLastName(lastName);
        party.setEmailAddress(email);
        return party;
    }

    private PartyEntity createParty() {
        return createParty("John", "Doe", "test@example.com");
    }

    private PcsCaseEntity createCase() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(1234567890L);
        return pcsCase;
    }

    private PaymentAgreementEntity createPaymentAgreement() {
        PaymentAgreementEntity paymentAgreement = new PaymentAgreementEntity();
        paymentAgreement.setId(UUID.randomUUID());
        paymentAgreement.setAnyPaymentsMade(VerticalYesNo.YES);
        return paymentAgreement;
    }

    private DefendantResponseEntity createDefendantResponse() {
        DefendantResponseEntity defendantResponse = new DefendantResponseEntity();
        defendantResponse.setId(UUID.randomUUID());
        defendantResponse.setParty(createParty());
        defendantResponse.setPcsCase(createCase());
        defendantResponse.setPaymentAgreement(createPaymentAgreement());

        ClaimEntity claim = new ClaimEntity();
        PartyEntity claimantParty = createParty("Jane", "Smith", "claimant@example.com");
        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .party(claimantParty)
            .role(PartyRole.CLAIMANT)
            .build();
        claim.setClaimParties(new java.util.ArrayList<>(java.util.List.of(claimParty)));
        claim.setFeePayments(new java.util.ArrayList<>());
        defendantResponse.setClaim(claim);

        return defendantResponse;
    }
}
