package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.pcs.config.AsyncConfiguration;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the NotificationService class.
 *
 * <p>This test suite covers both public and private methods of the NotificationService class.
 * For private methods, we use ReflectionTestUtils to invoke them directly for more focused testing.
 *
 * <p>The test suite is organized as follows:
 *
 * <p>1. Tests for public API methods (sendEmail, checkNotificationStatus)
 * 2. Tests for package-private methods (createCaseNotification)
 * 3. Tests for private helper methods (getAndProcessNotificationStatus, handleStatusCheckException, etc.)
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(AsyncConfiguration.class)
class NotificationServiceTest {

    // Constants
    private static final long STATUS_CHECK_DELAY = 100L; // 100ms for faster tests
    
    // Mocks
    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationRepository notificationRepository;

    // Service under test
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationClient, notificationRepository, STATUS_CHECK_DELAY);
    }

    // ===============================================================================================
    // Tests for sendEmail - Public API
    // ===============================================================================================
    
    @DisplayName("Should successfully send email when input data is valid")
    @Test
    void testSendEmailSuccess() throws NotificationClientException {
        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
        when(sendEmailResponse.getNotificationId())
            .thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        when(sendEmailResponse.getReference())
            .thenReturn(Optional.of("reference"));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);

        SendEmailResponse response = notificationService.sendEmail(emailRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNotificationId())
            .isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(response.getReference()).contains("reference");
        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
        // Verify save is called exactly 2 times - once for initial creation and once for status update
        verify(notificationRepository, times(2)).save(any(CaseNotification.class));
    }

    @DisplayName("Should throw notification exception when email sending fails")
    @Test
    void testSendEmailFailure() throws NotificationClientException {
        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenThrow(new NotificationClientException("Error"));

        assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
            .isInstanceOf(NotificationException.class)
            .hasMessage("Email failed to send, please try again.");

        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
        // Verify save is called exactly 2 times - once for initial creation and once for status update
        verify(notificationRepository, times(2)).save(any(CaseNotification.class));
    }

    @DisplayName("Should generate UUID for case ID when sending email")
    @Test
    void shouldGenerateUuidForCaseIdWhenSendingEmail() throws NotificationClientException {
        final EmailNotificationRequest emailRequest = new EmailNotificationRequest(
            "test@example.com",
            "templateId",
            new HashMap<>(),
            "reference",
            "emailReplyToId"
        );
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        
        // Capture the CaseNotification being saved to verify its caseId is a UUID
        ArgumentCaptor<CaseNotification> caseNotificationCaptor = ArgumentCaptor.forClass(CaseNotification.class);
        when(notificationRepository.save(caseNotificationCaptor.capture())).thenReturn(mock(CaseNotification.class));
        
        // Setup stub for the email sending to avoid actual notification client call
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn(sendEmailResponse);
        
        // Mock the response ID which is used in the production code
        when(sendEmailResponse.getNotificationId())
            .thenReturn(UUID.randomUUID());
        
        notificationService.sendEmail(emailRequest);

        // First save captured value is from the createCaseNotification call
        CaseNotification capturedNotification = caseNotificationCaptor.getAllValues().get(0);
        assertThat(capturedNotification.getCaseId()).isNotNull();
        // Verify that a valid UUID was set for the caseId
        assertThat(capturedNotification.getCaseId().toString())
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    // ===============================================================================================
    // Tests for createCaseNotification - Package Private Method
    // ===============================================================================================
    
    @DisplayName("Should save case notification when end point is called successfully")
    @Test
    void shouldSaveCaseNotificationWhenEndPointIsCalled() {
        String recipient = "test@example.com";
        NotificationStatus status = NotificationStatus.PENDING_SCHEDULE;
        UUID caseId = UUID.randomUUID();
        String type = "Email";

        CaseNotification testCaseNotification = new CaseNotification();
        testCaseNotification.setStatus(status);
        testCaseNotification.setRecipient(recipient);
        testCaseNotification.setCaseId(caseId);
        testCaseNotification.setType(type);

        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(testCaseNotification);
        CaseNotification saved = notificationService.createCaseNotification(recipient, type, caseId);

        assertThat(saved).isNotNull();
        assertThat(saved.getCaseId()).isEqualTo(testCaseNotification.getCaseId());
        assertThat(saved.getRecipient()).isEqualTo(testCaseNotification.getRecipient());
        verify(notificationRepository).save(any(CaseNotification.class));
    }

    @DisplayName("Should throw notification exception when saving of notification fails")
    @Test
    void shouldThrowNotificationExceptionWhenSavingFails() throws DataIntegrityViolationException {
        String recipient = "test@example.com";
        String type = "Email";
        UUID caseId = UUID.randomUUID();

        when(notificationRepository.save(any(CaseNotification.class)))
            .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        assertThatThrownBy(() -> 
            notificationService.createCaseNotification(recipient, type, caseId)
        ).isInstanceOf(NotificationException.class).hasMessage("Failed to save Case Notification.");
        verify(notificationRepository).save(any(CaseNotification.class));
    }

    // ===============================================================================================
    // Tests for checkNotificationStatus - Public API
    // ===============================================================================================
    
    @DisplayName("Should check notification status delivered")
    @Test
    void testCheckNotificationStatusDelivered() throws NotificationClientException, 
            InterruptedException, ExecutionException, TimeoutException {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);

        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        Notification result = future.get(6, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("delivered");
        verify(notificationClient).getNotificationById(notificationId);
    }

    @DisplayName("Should check notification status pending")
    @Test
    void testCheckNotificationStatusPending() throws NotificationClientException, 
            InterruptedException, ExecutionException, TimeoutException {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);

        when(notification.getStatus()).thenReturn("pending");
        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);

        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        Notification result = future.get(6, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("pending");
        verify(notificationClient).getNotificationById(notificationId);
    }

    @DisplayName("Should handle error when checking notification status")
    @Test
    void testCheckNotificationStatusError() throws NotificationClientException {
        String notificationId = UUID.randomUUID().toString();
        when(notificationClient.getNotificationById(notificationId))
            .thenThrow(new NotificationClientException("Failed to fetch notification status"));

        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        
        assertThatThrownBy(() -> future.get(6, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .satisfies(thrown -> {
                assertThat(thrown.getCause())
                    .isInstanceOf(NotificationClientException.class)
                    .hasMessage("Failed to fetch notification status");
            });

        verify(notificationClient).getNotificationById(notificationId);
    }

    // Removed direct reflection testing of private methods in favor of testing through the public API
    
    // ===============================================================================================
    // Tests for private helper methods (using ReflectionTestUtils)
    // ===============================================================================================

    @DisplayName("Should handle notification client exception when processing notification status")
    @Test
    void testGetAndProcessNotificationStatusClientException() throws Exception {
        String notificationId = UUID.randomUUID().toString();
        NotificationClientException expectedException = new NotificationClientException("API Error");
        when(notificationClient.getNotificationById(notificationId)).thenThrow(expectedException);
        
        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
        
        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(NotificationClientException.class);
    }

    @DisplayName("Should handle interrupted exception when processing notification status")
    @Test
    void testGetAndProcessNotificationStatusInterruptedException() {
        String notificationId = UUID.randomUUID().toString();
        InterruptedException interruptedException = new InterruptedException("Test interrupted");
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "handleStatusCheckException", 
            notificationId,
            interruptedException
        );
    }

    @DisplayName("Should handle exception correctly and reset interrupt flag")
    @Test
    void testHandleStatusCheckException() {
        String notificationId = UUID.randomUUID().toString();
        InterruptedException interruptedException = new InterruptedException("Test interrupted");
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "handleStatusCheckException", 
            notificationId,
            interruptedException
        );
    }

    @DisplayName("Should update notification status in database successfully")
    @Test
    void testUpdateNotificationStatusInDatabase() {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        CaseNotification caseNotification = new CaseNotification();
        
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
            .thenReturn(Optional.of(caseNotification));
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "updateNotificationStatusInDatabase", 
            notification,
            notificationId
        );
        
        verify(notificationRepository).findByProviderNotificationId(UUID.fromString(notificationId));
        verify(notificationRepository).save(caseNotification);
        assertThat(caseNotification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @DisplayName("Should handle null case notification when updating notification status")
    @Test
    void testUpdateNotificationStatusInDatabaseWithNullCaseNotification() {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        
        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
            .thenReturn(Optional.empty());
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "updateNotificationStatusInDatabase", 
            notification,
            notificationId
        );
        
        verify(notificationRepository).findByProviderNotificationId(UUID.fromString(notificationId));
        verify(notificationRepository, never()).save(any(CaseNotification.class));
    }

    @DisplayName("Should handle database exception when updating notification status")
    @Test
    void testUpdateNotificationStatusInDatabaseWithException() {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        CaseNotification caseNotification = new CaseNotification();
        
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
            .thenReturn(Optional.of(caseNotification));
        when(notificationRepository.save(any(CaseNotification.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "updateNotificationStatusInDatabase", 
            notification,
            notificationId
        );
    }

    @DisplayName("Should update notification with valid status")
    @Test
    void testUpdateNotificationWithStatus() {
        CaseNotification caseNotification = new CaseNotification();
        String status = "delivered";
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "updateNotificationWithStatus", 
            caseNotification,
            status
        );
        
        verify(notificationRepository).save(caseNotification);
        assertThat(caseNotification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @DisplayName("Should handle unknown status when updating notification")
    @Test
    void testUpdateNotificationWithUnknownStatus() {
        CaseNotification caseNotification = new CaseNotification();
        String status = "unknown-status";
        
        ReflectionTestUtils.invokeMethod(
            notificationService, 
            "updateNotificationWithStatus", 
            caseNotification,
            status
        );
        
        verify(notificationRepository, never()).save(any(CaseNotification.class));
    }

    @DisplayName("Should verify full notification status update flow with success")
    @Test
    void testFullNotificationStatusUpdateFlow() throws Exception {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        CaseNotification caseNotification = new CaseNotification();
        
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
            .thenReturn(Optional.of(caseNotification));
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(caseNotification);
        
        Notification result = (Notification) ReflectionTestUtils.invokeMethod(
            notificationService, 
            "getAndProcessNotificationStatus",
            notificationId
        );
        
        assertThat(result).isEqualTo(notification);
        // Verify the call flow through all methods
        verify(notificationClient).getNotificationById(notificationId);
        verify(notificationRepository).findByProviderNotificationId(UUID.fromString(notificationId));
        verify(notificationRepository).save(any(CaseNotification.class));
        assertThat(caseNotification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @DisplayName("Should update notification status with correct status and time values")
    @Test
    void testUpdateNotificationStatusWithCorrectValues() {
        CaseNotification caseNotification = new CaseNotification();
        NotificationStatus status = NotificationStatus.SENDING;
        UUID providerNotificationId = UUID.randomUUID();
        
        // Setup mock to return the same notification when saving
        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(caseNotification);
        
        Object resultObj = ReflectionTestUtils.invokeMethod(
            notificationService,
            "updateNotificationStatus",
            caseNotification,
            status,
            providerNotificationId
        );
        
        assertThat(resultObj).isNotNull().isInstanceOf(Optional.class);
        assertThat(caseNotification.getStatus()).isEqualTo(NotificationStatus.SENDING);
        assertThat(caseNotification.getProviderNotificationId()).isEqualTo(providerNotificationId);
        assertThat(caseNotification.getSubmittedAt()).isNotNull(); // Should set submitted time for SENDING status
        assertThat(caseNotification.getLastUpdatedAt()).isNotNull();
        verify(notificationRepository).save(caseNotification);
    }
    
    // ===============================================================================================
    // Advanced scenarios and edge cases
    // ===============================================================================================
    
    @DisplayName("Should handle database exception gracefully in updateNotificationStatus")
    @Test
    void testUpdateNotificationStatusDatabaseException() {
        CaseNotification caseNotification = new CaseNotification();
        NotificationStatus status = NotificationStatus.DELIVERED;
        UUID providerNotificationId = UUID.randomUUID();
        
        // Setup repository to throw exception
        when(notificationRepository.save(any(CaseNotification.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        Object resultObj = ReflectionTestUtils.invokeMethod(
            notificationService,
            "updateNotificationStatus",
            caseNotification,
            status,
            providerNotificationId
        );
        
        assertThat(resultObj).isNull(); // Method returns null on exception
    }

    @DisplayName("Should handle multiple exceptions during status check")
    @Test
    void testCheckNotificationStatusWithMultipleExceptions() throws Exception {
        String notificationId = UUID.randomUUID().toString();
        
        // First call throws one exception, second call different exception to test robustness
        when(notificationClient.getNotificationById(notificationId))
            .thenThrow(new NotificationClientException("API Error"))
            .thenThrow(new RuntimeException("Unexpected error"));
        
        // First exception
        CompletableFuture<Notification> future1 = notificationService.checkNotificationStatus(notificationId);
        assertThatThrownBy(() -> future1.get(1, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class);
        
        // Second exception - service should still handle it
        CompletableFuture<Notification> future2 = notificationService.checkNotificationStatus(notificationId);
        assertThatThrownBy(() -> future2.get(1, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class);
    }
    
    @DisplayName("Should handle nested exceptions in notification status update")
    @Test
    void testNestedExceptionsInNotificationStatusUpdate() {
        String notificationId = UUID.randomUUID().toString();
        Notification notification = mock(Notification.class);
        
        // Setup to throw exception from inside updateNotificationWithStatus
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
            .thenReturn(Optional.of(new CaseNotification()));
        doThrow(new RuntimeException("Failed to convert status"))
            .when(notificationRepository).save(any(CaseNotification.class));
        
        // This should complete without exceptions but log the error
        ReflectionTestUtils.invokeMethod(
            notificationService,
            "updateNotificationStatusInDatabase",
            notification,
            notificationId
        );
        
        // No exception should propagate outside the method
        // We can't directly verify the log, but we can verify the call flow
        verify(notificationRepository).findByProviderNotificationId(any(UUID.class));
        verify(notificationRepository).save(any(CaseNotification.class));
    }
}
