package uk.gov.hmcts.reform.pcs.hearings.messaging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmcHearingNotificationListenerTest {

    private static final String DEPLOYMENT_ID = "deployment-pcs-api-pr-1";
    private static final String MESSAGE_ID = "message-1";
    private static final int MAX_DELIVERY_COUNT = 10;

    private static final String NOTIFICATION = """
        {
          "hmctsServiceCode": "AAA3",
          "caseRef": "1234567890123456",
          "hearingID": "2000000001",
          "hearingUpdate": {
            "HMCStatus": "LISTED",
            "hearingListingStatus": "FIXED",
            "ListAssistCaseStatus": "LISTED",
            "hearingResponseReceivedDateTime": "2026-10-09T10:15:30",
            "unexpectedField": "ignored"
          }
        }
        """;

    @Mock
    private HmcHearingNotificationHandler handler;
    @Mock
    private ServiceBusReceivedMessageContext context;
    @Mock
    private ServiceBusReceivedMessage message;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private HmcHearingNotificationListener listener;
    private Logger listenerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        listener = new HmcHearingNotificationListener(objectMapper, handler, DEPLOYMENT_ID, "AAA3",
                                                      MAX_DELIVERY_COUNT);
        listenerLogger = (Logger) LoggerFactory.getLogger(HmcHearingNotificationListener.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        listenerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        listenerLogger.detachAppender(logAppender);
    }

    @Test
    void shouldRetrieveHearingAndCompleteMessageOnceWhenNotificationIsValid() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<HmcHearingResponse> captor = ArgumentCaptor.forClass(HmcHearingResponse.class);
        verify(handler).handle(captor.capture());
        assertThat(captor.getValue().getHearingID()).isEqualTo("2000000001");
        assertThat(captor.getValue().getCaseRef()).isEqualTo("1234567890123456");
        assertThat(captor.getValue().getHearingUpdate().getHmcStatus()).isEqualTo("LISTED");
        verify(context, times(1)).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter(any());
    }

    @Test
    void shouldCompleteMessageOnlyAfterHearingHasBeenRetrieved() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(handler, context);

        // when
        listener.onMessage(context);

        // then
        inOrder.verify(handler).handle(any());
        inOrder.verify(context).complete();
    }

    @Test
    void shouldDeadLetterNotificationAddressedToAnotherDeployment() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties("deployment-pcs-api-pr-2"));

        // when
        listener.onMessage(context);

        // then
        DeadLetterOptions options = captureDeadLetter();
        assertThat(options.getDeadLetterReason()).isEqualTo("HmctsDeploymentMismatch");
        assertThat(options.getDeadLetterErrorDescription())
            .isEqualTo("Message deployment deployment-pcs-api-pr-2 does not match " + DEPLOYMENT_ID);
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @Test
    void shouldDeadLetterNotificationWithoutDeploymentId() {
        // given
        givenMessage(NOTIFICATION, Map.of());

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("HmctsDeploymentMismatch");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldDeadLetterNotificationWithBlankDeploymentId(String messageDeploymentId) {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(messageDeploymentId));

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("HmctsDeploymentMismatch");
        verifyNoInteractions(handler);
    }

    @Test
    void shouldDeadLetterNotificationWithNonTextDeploymentId() {
        // given
        givenMessage(NOTIFICATION, Map.of("hmctsDeploymentId", 42));

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("HmctsDeploymentMismatch");
        verifyNoInteractions(handler);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldProcessNotificationWithoutDeploymentPropertyWhenNoDeploymentIdIsConfigured(String configured) {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, configured, "AAA3", MAX_DELIVERY_COUNT);
        givenMessage(NOTIFICATION, Map.of());

        // when
        listener.onMessage(context);

        // then
        verify(handler).handle(any());
        verify(context, times(1)).complete();
        verify(context, never()).deadLetter(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "deployment-pcs-api-pr-2", "any-other-deployment"})
    void shouldProcessNotificationWithAnyDeploymentPropertyWhenNoDeploymentIdIsConfigured(String messageValue) {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, "", "AAA3", MAX_DELIVERY_COUNT);
        givenMessage(NOTIFICATION, deploymentProperties(messageValue));

        // when
        listener.onMessage(context);

        // then
        verify(handler).handle(any());
        verify(context, times(1)).complete();
        verify(context, never()).deadLetter(any());
    }

    @Test
    void shouldProcessNotificationWithNonTextDeploymentPropertyWhenNoDeploymentIdIsConfigured() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, null, "AAA3", MAX_DELIVERY_COUNT);
        givenMessage(NOTIFICATION, Map.of("hmctsDeploymentId", 42));

        // when
        listener.onMessage(context);

        // then
        verify(handler).handle(any());
        verify(context, times(1)).complete();
    }

    @Test
    void shouldStillRejectInvalidNotificationWhenNoDeploymentIdIsConfigured() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, "", "AAA3", MAX_DELIVERY_COUNT);
        givenMessage(NOTIFICATION.replace("AAA3", "BBA3"), Map.of());

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("UnexpectedHmctsServiceCode");
        verifyNoInteractions(handler);
    }

    @Test
    void shouldLogMatchingOutcomeWithoutDeploymentWhenNoDeploymentIdIsConfigured() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, "", "AAA3", MAX_DELIVERY_COUNT);
        givenMessage(NOTIFICATION, Map.of("hmctsServiceId", "AAA3"));

        // when
        listener.onMessage(context);

        // then
        assertThat(logEvent("Received HMC message").getFormattedMessage()).contains(MESSAGE_ID, "AAA3");
        assertThat(logEvent("Completed HMC message").getFormattedMessage()).contains(MESSAGE_ID, "2000000001");
    }

    @Test
    void shouldNotReadBodyOfNotificationForAnotherDeployment() {
        // given
        givenMessage("not json", deploymentProperties("deployment-pcs-api-pr-2"));

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("HmctsDeploymentMismatch");
        verify(message, never()).getBody();
    }

    @Test
    void shouldDeadLetterUnreadableNotification() {
        // given
        givenMessage("not json", deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        DeadLetterOptions options = captureDeadLetter();
        assertThat(options.getDeadLetterReason()).isEqualTo("UnreadableHmcNotification");
        assertThat(options.getDeadLetterErrorDescription()).doesNotContain("not json");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
        verify(context, never()).abandon();
    }

    @ParameterizedTest
    @MethodSource("unusableNotificationBodies")
    void shouldDeadLetterNotificationWithUnusableBody(String body, String reason) {
        // given
        givenMessage(body, deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo(reason);
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    private static Stream<Arguments> unusableNotificationBodies() {
        return Stream.of(
            Arguments.of("", "UnreadableHmcNotification"),
            Arguments.of("null", "UnreadableHmcNotification"),
            Arguments.of("{\"hmctsServiceCode\": \"AAA3\", \"caseRef\": \"1234567890123456\"}",
                        "InvalidHmcNotification"),
            Arguments.of("{\"hmctsServiceCode\": \"AAA3\", \"hearingID\": \"2000000001\"}",
                        "InvalidHmcNotification")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void shouldDeadLetterNotificationWithBlankIdentifiers(String blank) {
        // given
        givenMessage("{\"hmctsServiceCode\": \"AAA3\", \"caseRef\": \"" + blank + "\", \"hearingID\": \"" + blank
                         + "\"}", deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("InvalidHmcNotification");
        verifyNoInteractions(handler);
    }

    @Test
    void shouldDeadLetterNotificationForAnotherService() {
        // given
        givenMessage(NOTIFICATION.replace("AAA3", "BBA3"), deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        DeadLetterOptions options = captureDeadLetter();
        assertThat(options.getDeadLetterReason()).isEqualTo("UnexpectedHmctsServiceCode");
        assertThat(options.getDeadLetterErrorDescription())
            .isEqualTo("Expected service code AAA3 but received BBA3");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @Test
    void shouldDeadLetterNotificationWithoutServiceCode() {
        // given
        givenMessage("{\"caseRef\": \"1234567890123456\", \"hearingID\": \"2000000001\"}",
                     deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        assertThat(captureDeadLetter().getDeadLetterReason()).isEqualTo("UnexpectedHmctsServiceCode");
        verifyNoInteractions(handler);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 422})
    void shouldDeadLetterNotificationWhenHmcRejectsPermanently(int status) {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        doThrow(feignException(status)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        DeadLetterOptions options = captureDeadLetter();
        assertThat(options.getDeadLetterReason()).isEqualTo("HmcRejectedRequest");
        assertThat(options.getDeadLetterErrorDescription()).isEqualTo("HMC responded " + status);
        verify(context, never()).abandon();
        verify(context, never()).complete();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 401, 403, 408, 429, 500, 503})
    void shouldLeaveNotificationUnsettledWhenHmcFailureMayBeTransient(int status) {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        doThrow(feignException(status)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        verifyNotSettled();
    }

    @Test
    void shouldLeaveNotificationUnsettledWhenProcessingFailsUnexpectedly() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        doThrow(new IllegalStateException("HMC unavailable")).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        verifyNotSettled();
    }

    @Test
    void shouldDeadLetterNotificationWhenRetriesAreExhausted() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        when(message.getDeliveryCount()).thenReturn((long) MAX_DELIVERY_COUNT - 1);
        doThrow(feignException(503)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        DeadLetterOptions options = captureDeadLetter();
        assertThat(options.getDeadLetterReason()).isEqualTo("HmcRetrievalRetriesExhausted");
        assertThat(options.getDeadLetterErrorDescription()).isEqualTo("HMC responded 503");
        verify(context, never()).complete();
    }

    @Test
    void shouldDeadLetterNotificationWhenRetriesAreExhaustedByUnexpectedFailure() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        when(message.getDeliveryCount()).thenReturn((long) MAX_DELIVERY_COUNT);
        doThrow(new IllegalStateException("HMC unavailable")).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        DeadLetterOptions options = captureDeadLetter();
        assertThat(options.getDeadLetterReason()).isEqualTo("HmcRetrievalRetriesExhausted");
        assertThat(options.getDeadLetterErrorDescription()).isEqualTo("IllegalStateException");
    }

    @Test
    void shouldKeepRetryingWhileDeliveryCountIsBelowLimit() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        when(message.getDeliveryCount()).thenReturn((long) MAX_DELIVERY_COUNT - 2);
        doThrow(feignException(503)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        verifyNotSettled();
    }

    @Test
    void shouldNotPropagateFailureToCompleteMessage() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        doThrow(new ServiceBusException(new IllegalStateException("lock lost secret"),
                                        ServiceBusErrorSource.COMPLETE)).when(context).complete();

        // when
        // then
        assertThatCode(() -> listener.onMessage(context)).doesNotThrowAnyException();
        verify(handler).handle(any());
        verify(context, times(1)).complete();
        verify(context, never()).deadLetter(any());
        ILoggingEvent event = logEvent("Failed to complete");
        assertThat(event.getFormattedMessage()).contains(MESSAGE_ID, "2000000001", "GENERAL_ERROR")
            .doesNotContain("lock lost secret");
    }

    @Test
    void shouldNotPropagateFailureToDeadLetterMessage() {
        // given
        givenMessage("not json", deploymentProperties(DEPLOYMENT_ID));
        doThrow(new IllegalStateException("broker unavailable")).when(context).deadLetter(any());

        // when
        // then
        assertThatCode(() -> listener.onMessage(context)).doesNotThrowAnyException();
        verify(context, times(1)).deadLetter(any());
        verify(context, never()).complete();
        assertThat(logEvent("Failed to dead-letter").getFormattedMessage())
            .contains(MESSAGE_ID, "UnreadableHmcNotification", "IllegalStateException");
    }

    @Test
    void shouldLogReceiptValidationRetrievalAndCompletionOutcomes() {
        // given
        givenMessage(NOTIFICATION, Map.of("hmctsDeploymentId", DEPLOYMENT_ID, "hmctsServiceId", "AAA3"));

        // when
        listener.onMessage(context);

        // then
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anySatisfy(line -> assertThat(line).contains("Received HMC message", MESSAGE_ID, "AAA3", DEPLOYMENT_ID))
            .anySatisfy(line -> assertThat(line).contains("validated", MESSAGE_ID, "2000000001", "1234567890123456"))
            .anySatisfy(line -> assertThat(line).contains("Completed HMC message", MESSAGE_ID, "2000000001"));
    }

    @Test
    void shouldLogDeploymentMismatchWithoutLoggingPayload() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties("deployment-pcs-api-pr-2"));

        // when
        listener.onMessage(context);

        // then
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anySatisfy(line -> assertThat(line).contains("does not match this deployment", MESSAGE_ID,
                                                          "deployment-pcs-api-pr-2", DEPLOYMENT_ID))
            .anySatisfy(line -> assertThat(line).contains("Dead-lettered", "HmctsDeploymentMismatch"))
            .noneSatisfy(line -> assertThat(line).contains("1234567890123456"));
    }

    @Test
    void shouldLogUnreadableNotificationWithoutPayloadOrStackTrace() {
        // given
        givenMessage("secret-not-json", deploymentProperties(DEPLOYMENT_ID));

        // when
        listener.onMessage(context);

        // then
        ILoggingEvent event = logEvent("could not be read");
        assertThat(event.getFormattedMessage()).contains(MESSAGE_ID).doesNotContain("secret-not-json");
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    void shouldLogPermanentHmcFailureWithStatusAndWithoutException() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        doThrow(feignException(404)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        ILoggingEvent event = logEvent("HMC rejected retrieval");
        assertThat(event.getFormattedMessage()).contains(MESSAGE_ID, "2000000001", "404");
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    void shouldLogTransientFailureWhenLeavingMessageForRedelivery() {
        // given
        givenMessage(NOTIFICATION, deploymentProperties(DEPLOYMENT_ID));
        doThrow(feignException(503)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        assertThat(logEvent("unsettled for redelivery").getFormattedMessage())
            .contains(MESSAGE_ID, "2000000001", "HMC responded 503");
    }

    @Test
    void shouldLogServiceBusFailureReasonWithoutExceptionWhenReportingError() {
        // given
        ServiceBusErrorContext errorContext = mock(ServiceBusErrorContext.class);
        when(errorContext.getEntityPath()).thenReturn("hmc-to-cft-aat/subscriptions/pcs");
        when(errorContext.getException()).thenReturn(
            new ServiceBusException(new IllegalStateException("connection lost"), ServiceBusErrorSource.RECEIVE));

        // when
        listener.onError(errorContext);

        // then
        ILoggingEvent event = logAppender.list.getFirst();
        assertThat(event.getFormattedMessage())
            .contains("hmc-to-cft-aat/subscriptions/pcs")
            .contains("GENERAL_ERROR")
            .doesNotContain("connection lost");
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    void shouldLogExceptionTypeWithoutExceptionWhenReportingNonServiceBusError() {
        // given
        ServiceBusErrorContext errorContext = mock(ServiceBusErrorContext.class);
        when(errorContext.getException()).thenReturn(new IllegalStateException("connection lost"));

        // when
        listener.onError(errorContext);

        // then
        ILoggingEvent event = logAppender.list.getFirst();
        assertThat(event.getFormattedMessage())
            .contains("IllegalStateException")
            .doesNotContain("connection lost");
        assertThat(event.getThrowableProxy()).isNull();
    }

    private DeadLetterOptions captureDeadLetter() {
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context, times(1)).deadLetter(captor.capture());
        return captor.getValue();
    }

    private ILoggingEvent logEvent(String fragment) {
        return logAppender.list.stream()
            .filter(event -> event.getFormattedMessage().contains(fragment))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No log line containing: " + fragment));
    }

    private void verifyNotSettled() {
        verify(context, never()).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter(any());
    }

    private static Map<String, Object> deploymentProperties(String deploymentId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hmctsDeploymentId", deploymentId);
        return properties;
    }

    private static FeignException feignException(int status) {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(status);
        return exception;
    }

    private void givenMessage(String body, Map<String, Object> applicationProperties) {
        lenient().when(context.getMessage()).thenReturn(message);
        lenient().when(message.getMessageId()).thenReturn(MESSAGE_ID);
        lenient().when(message.getApplicationProperties()).thenReturn(applicationProperties);
        lenient().when(message.getBody()).thenReturn(BinaryData.fromString(body));
    }
}
