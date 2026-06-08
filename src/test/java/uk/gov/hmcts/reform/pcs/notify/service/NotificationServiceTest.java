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
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.config.NotificationTemplateConfiguration;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationType;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;

import uk.gov.hmcts.reform.pcs.notify.model.NotificationRecipient;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.TemplatePersonalisation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private PartyService partyService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private NotificationPersonalisationFactory notificationPersonalisationFactory;

    private NotificationService notificationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEMPLATE_ID = "template-123";
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID PROVIDER_NOTIFICATION_ID = UUID.randomUUID();
    private static final String STATUS_STRING = "delivered";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationRepository,
            partyService,
            schedulerClient,
            templateConfiguration,
            notificationPersonalisationFactory,
            pcsCaseService
        );
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

            EmailNotificationResponse response =
                notificationService.scheduleEmailNotification(request, createCase(), new ClaimEntity(), createParty());

            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());
            assertThat(response.getNotificationId()).isEqualTo(savedNotification.getId());

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

            EmailNotificationResponse response =
                notificationService.scheduleEmailNotification(request, createCase(), new ClaimEntity(), createParty());

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

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(
                request, createCase(), new ClaimEntity(), createParty()
            );

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

            assertThatThrownBy(() -> notificationService
                .scheduleEmailNotification(request, createCase(), new ClaimEntity(), createParty()))
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

            notificationService.scheduleEmailNotification(request, createCase(), new ClaimEntity(), createParty());

            ArgumentCaptor<CaseNotification> notificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            CaseNotification firstSave = notificationCaptor.getAllValues().getFirst();
            assertThat(firstSave.getStatus()).isEqualTo(NotificationStatus.PENDING_SCHEDULE);
            assertThat(firstSave.getType()).isEqualTo(NotificationType.EMAIL);
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
            NotificationService service = new NotificationService(
                notificationRepository,
                partyService,
                schedulerClient,
                templateConfiguration,
                notificationPersonalisationFactory,
                pcsCaseService
            );

            assertThat(service).isNotNull();
        }
    }

    @Test
    @DisplayName("Should set submittedAt when status is SUBMITTED")
    void shouldSetSubmittedAtWhenStatusIsSubmitted() {
        CaseNotification notification = createCaseNotification();
        notification.setSubmittedAt(null);

        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(notification);

        notificationService.updateNotificationStatus(NOTIFICATION_ID, "SUBMITTED");

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

            ContactPreferencesEntity preferences = new ContactPreferencesEntity();
            preferences.setContactByEmail(VerticalYesNo.YES);
            party.setContactPreferences(preferences);

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
            claim.setClaimParties(new ArrayList<>(List.of(claimParty)));
            defendantResponse.setClaim(claim);

            lenient().when(notificationPersonalisationFactory.forDefendant(any()))
                .thenReturn(BasePersonalisation.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .caseNumber("1234567890")
                    .claimantName("JANE SMITH")
                    .primaryDefendantName("JOHN DOE")
                    .build());

            lenient().when(notificationPersonalisationFactory.counterclaimSuccess(any()))
                .thenReturn(CounterclaimPaymentSuccessPersonalisation.builder()
                    .base(BasePersonalisation.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .caseNumber("1234567890")
                        .claimantName("JANE SMITH")
                        .primaryDefendantName("JOHN DOE")
                        .build())
                    .paymentReferenceNumber("PAY-123")
                    .build());
            lenient().when(notificationPersonalisationFactory
                               .forClaimant(any()))
                .thenReturn(BasePersonalisation.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .caseNumber("1234567890")
                    .claimantName("JANE SMITH")
                    .primaryDefendantName("JOHN DOE")
                    .build());
            lenient().when(notificationPersonalisationFactory
                               .forClaimant(anyLong(), any(PCSCase.class)))
                .thenReturn(ClaimantBasePersonalisation.builder()
                    .toLineClaimantName("Jane Smith")
                    .caseNumber("1234567890")
                    .claimantName("JANE SMITH")
                    .primaryDefendantName("JOHN DOE")
                    .build());
        }

        @Test
        @DisplayName("Should send claimant defendant has made counterclaim email")
        void shouldSendClaimantDefendantHasMadeCounterclaimEmail() {
            PartyEntity claimantParty = new PartyEntity();
            claimantParty.setEmailAddress(TEST_EMAIL);
            when(partyService.getPrimaryClaimantPartyEntity(any())).thenReturn(claimantParty);
            when(partyService.canSendEmailNotification(any(), eq(PartyRole.CLAIMANT))).thenReturn(true);

            when(templateConfiguration.getTemplateId(EmailTemplate.MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendClaimantDefendantHasMadeCounterclaimEmail(defendantResponse.getClaim());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(templateConfiguration).getTemplateId(EmailTemplate.MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM);
            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should throw PartyNotFoundException when claimant is null in claimantRecipient")
        void shouldThrowPartyNotFoundExceptionWhenClaimantIsNullInClaimantRecipient() {
            when(partyService.getPrimaryClaimantPartyEntity(any())).thenReturn(null);

            UUID claimId = UUID.randomUUID();
            ClaimEntity claim = defendantResponse.getClaim();
            claim.setId(claimId);

            assertThatThrownBy(() -> notificationService.sendClaimantDefendantHasMadeCounterclaimEmail(claim))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessage("No claimant party found for claim: " + claimId);

            verify(partyService).getPrimaryClaimantPartyEntity(any());
            verifyNoInteractions(templateConfiguration);
            verifyNoInteractions(notificationRepository);
            verifyNoInteractions(schedulerClient);
        }

        @Test
        @DisplayName("Should throw PartyNotFoundException when defendant is null in defendantRecipient")
        void shouldThrowPartyNotFoundExceptionWhenDefendantIsNullInDefendantRecipient() {
            defendantResponse.setParty(null);
            UUID responseId = UUID.randomUUID();
            defendantResponse.setId(responseId);

            assertThatThrownBy(
                () -> notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessage("No defendant party found for response: " + responseId);

            verifyNoInteractions(partyService);
            verifyNoInteractions(templateConfiguration);
            verifyNoInteractions(notificationRepository);
            verifyNoInteractions(schedulerClient);
        }

        @Test
        @DisplayName("Should send defendant response no counterclaim email")
        void shouldSendDefendantResponseNoCounterclaimEmail() {
            when(partyService.canSendEmailNotification(any(), any())).thenReturn(true);
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
            when(partyService.canSendEmailNotification(any(), any())).thenReturn(true);
            when(templateConfiguration.getTemplateId(
                EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(
                    defendantResponse
                );

            assertThat(response).isNotNull();

            verify(templateConfiguration)
                .getTemplateId(EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED);
        }

        @Test
        @DisplayName("Should send counterclaim payment success email")
        void shouldSendCounterclaimPaymentSuccessEmail() {
            when(partyService.canSendEmailNotification(any(), any())).thenReturn(true);
            when(templateConfiguration.getTemplateId(
                EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS))
                .thenReturn(TEMPLATE_ID);

            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-123")
                .build();
            defendantResponse.getClaim().setFeePayment(feePayment);

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
            when(partyService.canSendEmailNotification(any(), any())).thenReturn(true);
            when(templateConfiguration.getTemplateId(
                EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(
                    defendantResponse
                );

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(templateConfiguration)
                .getTemplateId(EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED);

            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should NOT send email when canSendEmailNotification is false for defendant")
        void shouldNotSendEmailWhenCanSendEmailNotificationIsFalseForDefendant() {
            when(partyService.canSendEmailNotification(any(), any())).thenReturn(false);

            EmailNotificationResponse response =
                notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);

            assertThat(response).isNull();

            verifyNoInteractions(templateConfiguration, notificationRepository, schedulerClient);
        }

        @Test
        @DisplayName("Should send email when sending to claimant")
        void shouldSendEmailWhenCanSendEmailNotificationIsFalseForClaimant() {
            PartyEntity claimantParty = new PartyEntity();
            claimantParty.setEmailAddress(TEST_EMAIL);
            when(partyService.getPrimaryClaimantPartyEntity(any())).thenReturn(claimantParty);
            when(partyService.canSendEmailNotification(any(), eq(PartyRole.CLAIMANT))).thenReturn(true);

            when(templateConfiguration.getTemplateId(EmailTemplate.MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendClaimantDefendantHasMadeCounterclaimEmail(defendantResponse.getClaim());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(partyService).canSendEmailNotification(any(), eq(PartyRole.CLAIMANT));
            verify(templateConfiguration).getTemplateId(EmailTemplate.MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM);
            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }
    }

    @Nested
    @DisplayName("Claimant Draft Saved For Later Tests")
    class ClaimantDraftSavedForLaterTests {
        @BeforeEach
        void setUp() {
            lenient().when(partyService.canSendEmailNotification(any(), eq(PartyRole.CLAIMANT)))
                .thenReturn(true);
            lenient().when(notificationPersonalisationFactory.forClaimant(anyLong(), any()))
                .thenReturn(ClaimantBasePersonalisation.builder()
                    .toLineClaimantName("Jane Smith")
                    .caseNumber("1234567890")
                    .claimantName("JANE SMITH")
                    .primaryDefendantName("JOHN DOE")
                    .build());
        }

        @Test
        @DisplayName("Should use claimant email when contact email flag is YES")
        void shouldUseClaimantEmailWhenFlagIsYes() {
            when(templateConfiguration.getTemplateId(
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.YES,
                "claimant@example.com",
                "override@example.com",
                VerticalYesNo.YES,
                "Jane Smith",
                "Override Name"
            );

            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            notificationService.sendClaimantDraftSavedForLater(1234567890L, pcsCase);

            ArgumentCaptor<CaseNotification> captor =
                ArgumentCaptor.forClass(CaseNotification.class);

            verify(notificationRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().getFirst().getRecipient())
                .isEqualTo("claimant@example.com");
        }

        @Test
        @DisplayName("Should use overridden claimant email when contact email flag is NO")
        void shouldUseOverriddenClaimantEmailWhenFlagIsNo() {
            when(templateConfiguration.getTemplateId(
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.NO,
                "claimant@example.com",
                "override@example.com",
                VerticalYesNo.YES,
                "Jane Smith",
                "Override Name"
            );

            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            notificationService.sendClaimantDraftSavedForLater(1234567890L, pcsCase);

            ArgumentCaptor<CaseNotification> captor =
                ArgumentCaptor.forClass(CaseNotification.class);

            verify(notificationRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().getFirst().getRecipient())
                .isEqualTo("override@example.com");
        }

        @Test
        @DisplayName("Should default to claimant email when contact email flag is null")
        void shouldDefaultToClaimantEmailWhenFlagIsNull() {
            when(templateConfiguration.getTemplateId(
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();
            PCSCase pcsCase = createPcsCase(
                null,
                "claimant@example.com",
                "override@example.com",
                VerticalYesNo.YES,
                "Jane Smith",
                "Override Name"
            );

            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            notificationService.sendClaimantDraftSavedForLater(1234567890L, pcsCase);

            ArgumentCaptor<CaseNotification> captor =
                ArgumentCaptor.forClass(CaseNotification.class);

            verify(notificationRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().getFirst().getRecipient())
                .isEqualTo("claimant@example.com");
        }

        @Test
        @DisplayName("Should send claimant saved for later email")
        void shouldSendClaimantSavedForLaterEmail() {
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.YES,
                "claimant@example.com",
                "override@example.com",
                VerticalYesNo.YES,
                "Jane Smith",
                "Override Name"
            );

            when(templateConfiguration.getTemplateId(
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER))
                .thenReturn(TEMPLATE_ID);

            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            EmailNotificationResponse response =
                notificationService.sendClaimantDraftSavedForLater(1234567890L, pcsCase);

            assertThat(response).isNotNull();
            assertThat(response.getStatus())
                .isEqualTo(NotificationStatus.SCHEDULED.toString());

            verify(templateConfiguration)
                .getTemplateId(EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER);

            verify(notificationRepository, times(2)).save(any());
            verify(schedulerClient).scheduleIfNotExists(any());
        }

        @Test
        @DisplayName("Should skip claimant email when claimant email is null")
        void shouldSkipClaimantEmailWhenEmailIsNull() {
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.YES,
                null,
                null,
                VerticalYesNo.YES,
                "Jane Smith",
                "Override Name"
            );

            EmailNotificationResponse response =
                notificationService.sendClaimantDraftSavedForLater(1234567890L, pcsCase);

            assertThat(response).isNull();

            verifyNoInteractions(notificationRepository, schedulerClient);
        }

        @Test
        @DisplayName("Should not throw exception when draft saved email fails")
        void shouldNotThrowExceptionWhenDraftSavedEmailFails() {
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.YES,
                TEST_EMAIL,
                null,
                VerticalYesNo.YES,
                "Jane Smith",
                "Override Name"
            );

            when(templateConfiguration.getTemplateId(any())).thenThrow(new RuntimeException("Config error"));

            EmailNotificationResponse response =
                notificationService.sendClaimantDraftSavedForLater(1234567890L, pcsCase);

            assertThat(response).isNull();
        }
    }

    @Nested
    @DisplayName("TemplatePersonalisation Method Tests")
    class TemplatePersonalisationMethodTests {
        private final NotificationPersonalisationFactory factory = new NotificationPersonalisationFactory(partyService);

        @Test
        @DisplayName("Should use overridden claimant name when name flag is NO")
        void shouldUseOverriddenClaimantNameWhenFlagIsNo() {
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.YES,
                "claimant@example.com",
                "override@example.com",
                VerticalYesNo.NO,
                "Jane Smith",
                "Override Name"
            );

            Map<String, Object> result =
                factory.forClaimant(1234567890L, pcsCase).toMap();

            assertThat(result)
                .containsEntry("toLineClaimantName", "Override Name")
                .containsEntry("claimantName", "OVERRIDE NAME");
        }

        @Test
        @DisplayName("Should default to claimant name when name flag is null")
        void shouldDefaultToClaimantNameWhenFlagIsNull() {
            PCSCase pcsCase = createPcsCase(
                VerticalYesNo.YES,
                "claimant@example.com",
                "override@example.com",
                null,
                "Jane Smith",
                "Override Name"
            );

            Map<String, Object> result =
                factory.forClaimant(1234567890L, pcsCase).toMap();

            assertThat(result)
                .containsEntry("toLineClaimantName", "Jane Smith")
                .containsEntry("claimantName", "JANE SMITH");
        }

        @Test
        @DisplayName("Should persist claim type into notification")
        void shouldPersistClaimTypeIntoNotification() {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .claimType(NotificationClaimType.COUNTER_CLAIM)
                .build();

            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            notificationService.scheduleEmailNotification(
                request,
                createCase(),
                new ClaimEntity(),
                createParty()
            );

            ArgumentCaptor<CaseNotification> captor =
                ArgumentCaptor.forClass(CaseNotification.class);

            verify(notificationRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().getFirst().getClaimType())
                .isEqualTo(NotificationClaimType.COUNTER_CLAIM);
        }

        @Test
        @DisplayName("Should persist pcsCase claim and party into notification")
        void shouldPersistPcsCaseClaimAndPartyIntoNotification() {
            EmailNotificationRequest request = createValidEmailRequest();

            PcsCaseEntity pcsCase = createCase();
            ClaimEntity claim = new ClaimEntity();
            PartyEntity party = createParty();

            CaseNotification savedNotification = createCaseNotification();

            when(notificationRepository.save(any())).thenReturn(savedNotification);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

            notificationService.scheduleEmailNotification(
                request,
                pcsCase,
                claim,
                party
            );

            ArgumentCaptor<CaseNotification> captor =
                ArgumentCaptor.forClass(CaseNotification.class);

            verify(notificationRepository, times(2)).save(captor.capture());

            CaseNotification saved = captor.getAllValues().getFirst();

            assertThat(saved.getPcsCase()).isEqualTo(pcsCase);
            assertThat(saved.getClaimId()).isEqualTo(claim);
            assertThat(saved.getPartyId()).isEqualTo(party);
        }
    }

    @Nested
    @DisplayName("Send Email Tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should skip email when both party and email are null")
        void shouldSkipEmailWhenBothPartyAndEmailAreNull() {
            NotificationRecipient recipient = new NotificationRecipient(
                null,
                null,
                mock(PcsCaseEntity.class),
                null,
                PartyRole.CLAIMANT
            );

            EmailNotificationResponse response = notificationService.sendEmail(
                recipient,
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER,
                NotificationClaimType.POSSESSION_CLAIM,
                mock(TemplatePersonalisation.class)
            );

            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should send email when party is null but email is present")
        void shouldSendEmailWhenPartyIsNullButEmailIsPresent() {
            NotificationRecipient recipient = new NotificationRecipient(
                TEST_EMAIL,
                null,
                createCase(),
                null,
                PartyRole.CLAIMANT
            );

            when(templateConfiguration.getTemplateId(any())).thenReturn(TEMPLATE_ID);
            when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);
            when(notificationRepository.save(any())).thenReturn(new CaseNotification());

            EmailNotificationResponse response = notificationService.sendEmail(
                recipient,
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER,
                NotificationClaimType.POSSESSION_CLAIM,
                mock(TemplatePersonalisation.class)
            );

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULED.toString());
        }

        @Test
        @DisplayName("Should skip email when party exists but cannot send email")
        void shouldSkipEmailWhenPartyCannotSendEmail() {
            PartyEntity party = createParty();
            NotificationRecipient recipient = new NotificationRecipient(
                TEST_EMAIL,
                party,
                createCase(),
                null,
                PartyRole.CLAIMANT
            );

            when(partyService.canSendEmailNotification(eq(party), eq(PartyRole.CLAIMANT))).thenReturn(false);

            EmailNotificationResponse response = notificationService.sendEmail(
                recipient,
                EmailTemplate.MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER,
                NotificationClaimType.POSSESSION_CLAIM,
                mock(TemplatePersonalisation.class)
            );

            assertThat(response).isNull();
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
        notification.setId(NOTIFICATION_ID);
        notification.setPcsCase(createCase());
        notification.setRecipient(TEST_EMAIL);
        notification.setType(NotificationType.EMAIL);
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
        claim.setClaimParties(new ArrayList<>(List.of(claimParty)));
        defendantResponse.setClaim(claim);

        return defendantResponse;
    }

    private PCSCase createPcsCase(
        VerticalYesNo emailFlag,
        String claimantEmail,
        String overriddenEmail,
        VerticalYesNo nameFlag,
        String claimantName,
        String overriddenName
    ) {
        ClaimantContactPreferences contactPreferences = new ClaimantContactPreferences();
        contactPreferences.setIsCorrectClaimantContactEmail(emailFlag);
        contactPreferences.setClaimantContactEmail(claimantEmail);
        contactPreferences.setOverriddenClaimantContactEmail(overriddenEmail);

        ClaimantInformation claimantInformation = new ClaimantInformation();
        claimantInformation.setIsClaimantNameCorrect(nameFlag);
        claimantInformation.setClaimantName(claimantName);
        claimantInformation.setOverriddenClaimantName(overriddenName);

        DefendantDetails defendant = new DefendantDetails();
        defendant.setFirstName("John");
        defendant.setLastName("Doe");

        return PCSCase.builder()
            .claimantContactPreferences(contactPreferences)
            .claimantInformation(claimantInformation)
            .defendant1(defendant)
            .build();
    }
}
