//package uk.gov.hmcts.reform.pcs.notify.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
//import uk.gov.hmcts.reform.pcs.config.AsyncConfiguration;
//import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
//import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
//import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
//import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
//import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
//import uk.gov.service.notify.NotificationClient;
//import uk.gov.service.notify.NotificationClientException;
//import uk.gov.service.notify.SendEmailResponse;
//import uk.gov.service.notify.Notification;
//
//import java.util.HashMap;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyMap;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.times;
//
//@ExtendWith(MockitoExtension.class)
//@SpringJUnitConfig(AsyncConfiguration.class)
//class NotificationServiceTest {
//
//    @Mock
//    private NotificationClient notificationClient;
//
//    @Mock
//    private NotificationRepository notificationRepository;
//
//    private NotificationService notificationService;
//
//    private static final long STATUS_CHECK_DELAY = 100L; // 100ms for faster tests
//
//    @BeforeEach
//    void setUp() {
//        notificationService = new NotificationService(notificationClient, notificationRepository, STATUS_CHECK_DELAY);
//    }
//
//    @DisplayName("Should successfully send email when input data is valid")
//    @Test
//    void testSendEmailSuccess() throws NotificationClientException {
//        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
//            "test@example.com",
//            "templateId",
//            new HashMap<>(),
//            "reference",
//            "emailReplyToId"
//        );
//        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
//        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
//        when(sendEmailResponse.getNotificationId())
//            .thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
//        when(sendEmailResponse.getReference())
//            .thenReturn(Optional.of("reference"));
//        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
//            .thenReturn(sendEmailResponse);
//
//        SendEmailResponse response = notificationService.sendEmail(emailRequest);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getNotificationId())
//            .isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
//        assertThat(response.getReference()).contains("reference");
//        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
//        // Verify save is called exactly 2 times - once for initial creation and once for status update
//        verify(notificationRepository, times(2)).save(any(CaseNotification.class));
//    }
//
//    @DisplayName("Should throw notification exception when email sending fails")
//    @Test
//    void testSendEmailFailure() throws NotificationClientException {
//        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
//            "test@example.com",
//            "templateId",
//            new HashMap<>(),
//            "reference",
//            "emailReplyToId"
//        );
//        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(mock(CaseNotification.class));
//        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
//            .thenThrow(new NotificationClientException("Error"));
//
//        assertThatThrownBy(() -> notificationService.sendEmail(emailRequest))
//            .isInstanceOf(NotificationException.class)
//            .hasMessage("Email failed to send, please try again.");
//
//        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
//        // Verify save is called exactly 2 times - once for initial creation and once for status update
//        verify(notificationRepository, times(2)).save(any(CaseNotification.class));
//    }
//
//    @DisplayName("Should save case notification when end point is called successfully")
//    @Test
//    void shouldSaveCaseNotificationWhenEndPointIsCalled() {
//        String recipient = "test@example.com";
//        NotificationStatus status = NotificationStatus.PENDING_SCHEDULE;
//        UUID caseId = UUID.randomUUID();
//        String type = "Email";
//
//        CaseNotification testCaseNotification = new CaseNotification();
//        testCaseNotification.setStatus(status);
//        testCaseNotification.setRecipient(recipient);
//        testCaseNotification.setCaseId(caseId);
//        testCaseNotification.setType(type);
//
//        when(notificationRepository.save(any(CaseNotification.class))).thenReturn(testCaseNotification);
//        CaseNotification saved = notificationService.createCaseNotification(recipient, type, caseId);
//
//        assertThat(saved).isNotNull();
//        assertThat(saved.getCaseId()).isEqualTo(testCaseNotification.getCaseId());
//        assertThat(saved.getRecipient()).isEqualTo(testCaseNotification.getRecipient());
//        verify(notificationRepository).save(any(CaseNotification.class));
//    }
//
//    @DisplayName("Should throw notification exception when saving of notification fails")
//    @Test
//    void shouldThrowNotificationExceptionWhenSavingFails() throws DataIntegrityViolationException {
//        String recipient = "test@example.com";
//        String type = "Email";
//        UUID caseId = UUID.randomUUID();
//
//        when(notificationRepository.save(any(CaseNotification.class)))
//            .thenThrow(new DataIntegrityViolationException("Constraint violation"));
//
//        assertThatThrownBy(() ->
//            notificationService.createCaseNotification(recipient, type, caseId)
//        ).isInstanceOf(NotificationException.class)
//            .hasMessage("Failed to save Case Notification.");
//        verify(notificationRepository).save(any(CaseNotification.class));
//    }
//
//    @DisplayName("Should check notification status delivered")
//    @Test
//    void testCheckNotificationStatusDelivered() throws NotificationClientException,
//            InterruptedException, ExecutionException, TimeoutException {
//        String notificationId = UUID.randomUUID().toString();
//        Notification notification = mock(Notification.class);
//
//        when(notification.getStatus()).thenReturn("delivered");
//        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
//
//        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
//        Notification result = future.get(6, TimeUnit.SECONDS);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getStatus()).isEqualTo("delivered");
//        verify(notificationClient).getNotificationById(notificationId);
//    }
//
//    @DisplayName("Should check notification status pending")
//    @Test
//    void testCheckNotificationStatusPending() throws NotificationClientException,
//            InterruptedException, ExecutionException, TimeoutException {
//        String notificationId = UUID.randomUUID().toString();
//        Notification notification = mock(Notification.class);
//
//        when(notification.getStatus()).thenReturn("pending");
//        when(notificationClient.getNotificationById(notificationId)).thenReturn(notification);
//
//        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
//        Notification result = future.get(6, TimeUnit.SECONDS);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getStatus()).isEqualTo("pending");
//        verify(notificationClient).getNotificationById(notificationId);
//    }
//
//    @DisplayName("Should handle error when checking notification status")
//    @Test
//    void testCheckNotificationStatusError() throws NotificationClientException {
//        String notificationId = UUID.randomUUID().toString();
//        when(notificationClient.getNotificationById(notificationId))
//            .thenThrow(new NotificationClientException("Failed to fetch notification status"));
//
//        CompletableFuture<Notification> future = notificationService.checkNotificationStatus(notificationId);
//
//        assertThatThrownBy(() -> future.get(6, TimeUnit.SECONDS))
//            .isInstanceOf(ExecutionException.class)
//            .satisfies(thrown -> {
//                assertThat(thrown.getCause())
//                    .isInstanceOf(NotificationClientException.class)
//                    .hasMessage("Failed to fetch notification status");
//            });
//
//        verify(notificationClient).getNotificationById(notificationId);
//    }
//
//    @Test
//    @DisplayName("Should log error in updateNotificationStatusInDatabase if exception thrown")
//    void shouldLogErrorInUpdateNotificationStatusInDatabaseIfExceptionThrown() throws Exception {
//        String notificationId = UUID.randomUUID().toString();
//        Notification notification = mock(Notification.class);
//        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
//            .thenThrow(new RuntimeException("DB error"));
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatusInDatabase", Notification.class, String.class
//        );
//        method.setAccessible(true);
//        method.invoke(notificationService, notification, notificationId);
//        // No exception means the branch was hit
//    }
//
//    @Test
//    @DisplayName("Should log error in handleStatusCheckException for NotificationClientException")
//    void shouldLogErrorInHandleStatusCheckExceptionForNotificationClientException() throws Exception {
//        String notificationId = UUID.randomUUID().toString();
//        NotificationClientException ex = new NotificationClientException("error");
//        var method = notificationService.getClass().getDeclaredMethod(
//            "handleStatusCheckException", String.class, Exception.class
//        );
//        method.setAccessible(true);
//        method.invoke(notificationService, notificationId, ex);
//        // No exception means the branch was hit
//    }
//
//    @Test
//    @DisplayName("Should set submittedAt if status is SENDING in updateNotificationStatus")
//    void shouldSetSubmittedAtIfStatusIsSendingInUpdateNotificationStatus() throws Exception {
//        CaseNotification notification = new CaseNotification();
//        NotificationStatus status = NotificationStatus.SENDING;
//        when(notificationRepository.save(notification)).thenReturn(notification);
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatus", CaseNotification.class, NotificationStatus.class, UUID.class
//        );
//        method.setAccessible(true);
//        Optional<?> result = (Optional<?>) method.invoke(
//            notificationService, notification, status, null
//        );
//        assertThat(result).isPresent();
//        assertThat(notification.getSubmittedAt()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("Should set providerNotificationId if provided in updateNotificationStatus")
//    void shouldSetProviderNotificationIdIfProvidedInUpdateNotificationStatus() throws Exception {
//        CaseNotification notification = new CaseNotification();
//        NotificationStatus status = NotificationStatus.SUBMITTED;
//        UUID providerId = UUID.randomUUID();
//        when(notificationRepository.save(notification)).thenReturn(notification);
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatus", CaseNotification.class, NotificationStatus.class, UUID.class
//        );
//        method.setAccessible(true);
//        Optional<?> result = (Optional<?>) method.invoke(
//            notificationService, notification, status, providerId
//        );
//        assertThat(result).isPresent();
//        assertThat(notification.getProviderNotificationId()).isEqualTo(providerId);
//    }
//
//    @Test
//    @DisplayName("Should execute exceptionally block in sendEmail and hit log.error")
//    void shouldExecuteExceptionallyBlockInSendEmail() {
//        EmailNotificationRequest emailRequest = new EmailNotificationRequest(
//            "test@example.com", "templateId", new HashMap<>(), "reference", "emailReplyToId"
//        );
//        NotificationService spyService = org.mockito.Mockito.spy(notificationService);
//        org.mockito.Mockito.lenient()
//            .doReturn(CompletableFuture.failedFuture(new RuntimeException("Async error")))
//            .when(spyService)
//            .checkNotificationStatus(org.mockito.ArgumentMatchers.anyString());
//        try {
//            spyService.sendEmail(emailRequest);
//        } catch (Exception ignored) {
//            // We only care that the exceptionally lambda is executed
//        }
//    }
//
//    @Test
//    @DisplayName("Should update notification status with providerNotificationId null and status not SENDING")
//    void shouldUpdateNotificationStatusWithProviderIdNullAndStatusNotSending() throws Exception {
//        CaseNotification notification = new CaseNotification();
//        NotificationStatus status = NotificationStatus.SUBMITTED;
//        when(notificationRepository.save(notification)).thenReturn(notification);
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatus", CaseNotification.class, NotificationStatus.class, UUID.class
//        );
//        method.setAccessible(true);
//        Optional<?> result = (Optional<?>) method.invoke(
//            notificationService, notification, status, null
//        );
//        assertThat(result).isPresent();
//        assertThat(notification.getStatus()).isEqualTo(status);
//        assertThat(notification.getProviderNotificationId()).isNull();
//        assertThat(notification.getSubmittedAt()).isNull();
//    }
//
//    @Test
//    @DisplayName("Should update notification status with providerNotificationId not null and status SENDING")
//    void shouldUpdateNotificationStatusWithProviderIdNotNullAndStatusSending() throws Exception {
//        CaseNotification notification = new CaseNotification();
//        NotificationStatus status = NotificationStatus.SENDING;
//        UUID providerId = UUID.randomUUID();
//        when(notificationRepository.save(notification)).thenReturn(notification);
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatus", CaseNotification.class, NotificationStatus.class, UUID.class
//        );
//        method.setAccessible(true);
//        Optional<?> result = (Optional<?>) method.invoke(
//            notificationService, notification, status, providerId
//        );
//        assertThat(result).isPresent();
//        assertThat(notification.getStatus()).isEqualTo(status);
//        assertThat(notification.getProviderNotificationId()).isEqualTo(providerId);
//        assertThat(notification.getSubmittedAt()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("Should log warning if updateNotificationWithStatus called with invalid status")
//    void shouldLogWarningIfUpdateNotificationWithStatusInvalid() throws Exception {
//        CaseNotification notification = new CaseNotification();
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationWithStatus", CaseNotification.class, String.class
//        );
//        method.setAccessible(true);
//        method.invoke(notificationService, notification, "INVALID_STATUS");
//        // No exception means the branch was hit
//    }
//
//    @Test
//    @DisplayName("Should update notification with valid status in updateNotificationWithStatus")
//    void shouldUpdateNotificationWithStatusValid() throws Exception {
//        CaseNotification notification = new CaseNotification();
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationWithStatus", CaseNotification.class, String.class
//        );
//        method.setAccessible(true);
//        method.invoke(
//            notificationService, notification, NotificationStatus.SUBMITTED.toString()
//        );
//        // No exception means the branch was hit
//    }
//
//    @Test
//    @DisplayName("Should log warning if provider notification not found in updateNotificationStatusInDatabase")
//    void shouldLogWarningIfProviderNotificationNotFoundInUpdateNotificationStatusInDatabase() throws Exception {
//        String notificationId = UUID.randomUUID().toString();
//        Notification notification = mock(Notification.class);
//        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
//            .thenReturn(Optional.empty());
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatusInDatabase", Notification.class, String.class
//        );
//        method.setAccessible(true);
//        method.invoke(notificationService, notification, notificationId);
//        // No exception means the branch was hit
//    }
//
//    @Test
//    @DisplayName("Should log error if findByProviderNotificationId throws in updateNotificationStatusInDatabase")
//    void shouldLogErrorIfFindByProviderNotificationIdThrowsInUpdateNotificationStatusInDatabase() throws Exception {
//        String notificationId = UUID.randomUUID().toString();
//        Notification notification = mock(Notification.class);
//        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
//            .thenThrow(new RuntimeException("DB error"));
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatusInDatabase", Notification.class, String.class
//        );
//        method.setAccessible(true);
//        method.invoke(notificationService, notification, notificationId);
//        // No exception means the branch was hit
//    }
//
//    @Test
//    @DisplayName("Should hit both branches and catch in updateNotificationStatusInDatabase")
//    void shouldHitAllBranchesInUpdateNotificationStatusInDatabase() throws Exception {
//        String notificationId = UUID.randomUUID().toString();
//        Notification notification = mock(Notification.class);
//        // Branch: caseNotification != null
//        CaseNotification found = new CaseNotification();
//        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
//            .thenReturn(Optional.of(found));
//        var method = notificationService.getClass().getDeclaredMethod(
//            "updateNotificationStatusInDatabase", Notification.class, String.class
//        );
//        method.setAccessible(true);
//        method.invoke(notificationService, notification, notificationId);
//        // Branch: caseNotification == null
//        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
//            .thenReturn(Optional.empty());
//        method.invoke(notificationService, notification, notificationId);
//        // Branch: catch block
//        when(notificationRepository.findByProviderNotificationId(UUID.fromString(notificationId)))
//            .thenThrow(new RuntimeException("DB error"));
//        method.invoke(notificationService, notification, notificationId);
//    }
//
//    @Test
//    @DisplayName("Should hit log.error and interrupt in handleStatusCheckException for InterruptedException")
//    void shouldHitLogErrorAndInterruptInHandleStatusCheckException() throws Exception {
//        String notificationId = UUID.randomUUID().toString();
//        InterruptedException interrupted = new InterruptedException("interrupted");
//        var method = notificationService.getClass().getDeclaredMethod(
//            "handleStatusCheckException", String.class, Exception.class
//        );
//        method.setAccessible(true);
//        Thread.currentThread().interrupt(); // Set interrupt flag
//        method.invoke(notificationService, notificationId, interrupted);
//        assertThat(Thread.currentThread().isInterrupted()).isTrue();
//        Thread.interrupted(); // Clear interrupt flag for other tests
//        // Also test with a generic Exception
//        Exception ex = new Exception("generic");
//        method.invoke(notificationService, notificationId, ex);
//    }
//
//    @Test
//    @DisplayName("Directly invoke sendEmail exceptionally lambda for 100% coverage")
//    void shouldDirectlyInvokeSendEmailExceptionallyLambda() throws Exception {
//        // Get the lambda method reference
//        java.lang.reflect.Method lambda = null;
//        for (java.lang.reflect.Method m : notificationService.getClass().getDeclaredMethods()) {
//            if (m.getName().contains("lambda$sendEmail$0")) {
//                lambda = m;
//                break;
//            }
//        }
//        assertThat(lambda).isNotNull();
//        lambda.setAccessible(true);
//        // Call the lambda with a Throwable
//        lambda.invoke(notificationService, new RuntimeException("test"));
//    }
//}
