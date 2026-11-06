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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmcHearingNotificationListenerTest {

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
        listener = new HmcHearingNotificationListener(objectMapper, handler, "", "AAA3");
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
    void shouldProcessAndCompleteNotificationWithoutDeploymentIdWhenNoneConfigured() {
        // given
        givenMessage(NOTIFICATION, Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<HmcHearingResponse> captor = ArgumentCaptor.forClass(HmcHearingResponse.class);
        verify(handler).handle(captor.capture());
        assertThat(captor.getValue().getHearingID()).isEqualTo("2000000001");
        assertThat(captor.getValue().getHearingUpdate().getHmcStatus()).isEqualTo("LISTED");
        verify(context).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter(any());
    }

    @Test
    void shouldProcessAndCompleteNotificationWhenDeploymentIdMatches() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, "deployment-pcs-api-pr-1", "AAA3");
        givenMessage(NOTIFICATION, Map.of("hmctsDeploymentId", "deployment-pcs-api-pr-1"));

        // when
        listener.onMessage(context);

        // then
        verify(handler).handle(any());
        verify(context).complete();
    }

    @Test
    void shouldCompleteWithoutProcessingNotificationForAnotherDeployment() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, "deployment-pcs-api-pr-1", "AAA3");
        givenOtherDeployment(Map.of("hmctsDeploymentId", "deployment-pcs-api-pr-2"));

        // when
        listener.onMessage(context);

        // then
        verifyNoInteractions(handler);
        verify(context).complete();
        verify(context, never()).deadLetter(any());
    }

    @Test
    void shouldCompleteWithoutProcessingDeploymentTaggedNotificationWhenNoDeploymentConfigured() {
        // given
        givenOtherDeployment(Map.of("hmctsDeploymentId", "deployment-pcs-api-pr-2"));

        // when
        listener.onMessage(context);

        // then
        verifyNoInteractions(handler);
        verify(context).complete();
    }

    @Test
    void shouldCompleteWithoutProcessingUntaggedNotificationWhenDeploymentConfigured() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, handler, "deployment-pcs-api-pr-1", "AAA3");
        givenOtherDeployment(Map.of());

        // when
        listener.onMessage(context);

        // then
        verifyNoInteractions(handler);
        verify(context).complete();
    }

    @Test
    void shouldDeadLetterUnreadableNotification() {
        // given
        givenMessage("not json", Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context).deadLetter(captor.capture());
        assertThat(captor.getValue().getDeadLetterReason()).isEqualTo("UnreadableHmcNotification");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
        verify(context, never()).abandon();
    }

    @Test
    void shouldDeadLetterNotificationWithoutHearingId() {
        // given
        givenMessage("{\"hmctsServiceCode\": \"AAA3\", \"caseRef\": \"1234567890123456\"}", Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context).deadLetter(captor.capture());
        assertThat(captor.getValue().getDeadLetterReason()).isEqualTo("InvalidHmcNotification");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @Test
    void shouldLeaveNotificationUnsettledWhenProcessingFails() {
        // given
        givenMessage(NOTIFICATION, Map.of());
        doThrow(new IllegalStateException("HMC unavailable")).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        verifyNotSettled();
    }

    @Test
    void shouldDeadLetterNotificationWithoutCaseReference() {
        // given
        givenMessage("{\"hmctsServiceCode\": \"AAA3\", \"hearingID\": \"2000000001\"}", Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context).deadLetter(captor.capture());
        assertThat(captor.getValue().getDeadLetterReason()).isEqualTo("InvalidHmcNotification");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @Test
    void shouldDeadLetterEmptyNotification() {
        // given
        givenMessage("null", Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context).deadLetter(captor.capture());
        assertThat(captor.getValue().getDeadLetterReason()).isEqualTo("UnreadableHmcNotification");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @Test
    void shouldDeadLetterNotificationForAnotherService() {
        // given
        givenMessage(NOTIFICATION.replace("AAA3", "BBA3"), Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context).deadLetter(captor.capture());
        assertThat(captor.getValue().getDeadLetterReason()).isEqualTo("UnexpectedHmctsServiceCode");
        assertThat(captor.getValue().getDeadLetterErrorDescription())
            .isEqualTo("Expected service code AAA3 but received BBA3");
        verifyNoInteractions(handler);
        verify(context, never()).complete();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 422})
    void shouldDeadLetterNotificationWhenHmcRejectsPermanently(int status) {
        // given
        givenMessage(NOTIFICATION, Map.of());
        doThrow(feignException(status)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<DeadLetterOptions> captor = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(context).deadLetter(captor.capture());
        assertThat(captor.getValue().getDeadLetterReason()).isEqualTo("HmcRejectedRequest");
        verify(context, never()).abandon();
        verify(context, never()).complete();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 408, 429, 500, 503})
    void shouldLeaveNotificationUnsettledWhenHmcFailureMayBeTransient(int status) {
        // given
        givenMessage(NOTIFICATION, Map.of());
        doThrow(feignException(status)).when(handler).handle(any());

        // when
        listener.onMessage(context);

        // then
        verifyNotSettled();
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

    private void verifyNotSettled() {
        verify(context, never()).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter(any());
    }

    private static FeignException feignException(int status) {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(status);
        return exception;
    }

    private void givenMessage(String body, Map<String, Object> applicationProperties) {
        givenOtherDeployment(applicationProperties);
        when(message.getBody()).thenReturn(BinaryData.fromString(body));
    }

    private void givenOtherDeployment(Map<String, Object> applicationProperties) {
        when(context.getMessage()).thenReturn(message);
        when(message.getApplicationProperties()).thenReturn(applicationProperties);
    }
}
