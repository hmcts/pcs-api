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
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private NotificationService notificationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEMPLATE_ID = "template-123";
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID PROVIDER_NOTIFICATION_ID = UUID.randomUUID();
    private static final String STATUS_STRING = "delivered";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, schedulerClient);
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

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(request);

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

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(request);

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

            EmailNotificationResponse response = notificationService.scheduleEmailNotification(request);

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

            assertThatThrownBy(() -> notificationService.scheduleEmailNotification(request))
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

            notificationService.scheduleEmailNotification(request);

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
            NotificationService service = new NotificationService(notificationRepository, schedulerClient);

            assertThat(service).isNotNull();
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
}
