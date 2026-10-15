package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    private HmcHearingNotificationProcessor processor;
    @Mock
    private ServiceBusReceivedMessageContext context;
    @Mock
    private ServiceBusReceivedMessage message;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private HmcHearingNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new HmcHearingNotificationListener(objectMapper, processor, "");
    }

    @Test
    void shouldProcessAndCompleteNotificationWithoutDeploymentIdWhenNoneConfigured() {
        // given
        givenMessage(NOTIFICATION, Map.of());

        // when
        listener.onMessage(context);

        // then
        ArgumentCaptor<HmcHearingResponse> captor = ArgumentCaptor.forClass(HmcHearingResponse.class);
        verify(processor).process(captor.capture());
        assertThat(captor.getValue().getHearingID()).isEqualTo("2000000001");
        assertThat(captor.getValue().getHearingUpdate().getHmcStatus()).isEqualTo("LISTED");
        verify(context).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter(any());
    }

    @Test
    void shouldProcessAndCompleteNotificationWhenDeploymentIdMatches() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, processor, "deployment-pcs-api-pr-1");
        givenMessage(NOTIFICATION, Map.of("hmctsDeploymentId", "deployment-pcs-api-pr-1"));

        // when
        listener.onMessage(context);

        // then
        verify(processor).process(any());
        verify(context).complete();
    }

    @Test
    void shouldCompleteWithoutProcessingNotificationForAnotherDeployment() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, processor, "deployment-pcs-api-pr-1");
        givenOtherDeployment(Map.of("hmctsDeploymentId", "deployment-pcs-api-pr-2"));

        // when
        listener.onMessage(context);

        // then
        verifyNoInteractions(processor);
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
        verifyNoInteractions(processor);
        verify(context).complete();
    }

    @Test
    void shouldCompleteWithoutProcessingUntaggedNotificationWhenDeploymentConfigured() {
        // given
        listener = new HmcHearingNotificationListener(objectMapper, processor, "deployment-pcs-api-pr-1");
        givenOtherDeployment(Map.of());

        // when
        listener.onMessage(context);

        // then
        verifyNoInteractions(processor);
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
        assertThat(captor.getValue().getDeadLetterReason())
            .isEqualTo(HmcHearingNotificationListener.UNREADABLE_REASON);
        verifyNoInteractions(processor);
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
        assertThat(captor.getValue().getDeadLetterReason())
            .isEqualTo(HmcHearingNotificationListener.MISSING_HEARING_ID_REASON);
        verifyNoInteractions(processor);
        verify(context, never()).complete();
    }

    @Test
    void shouldAbandonNotificationWhenProcessingFails() {
        // given
        givenMessage(NOTIFICATION, Map.of());
        doThrow(new IllegalStateException("HMC unavailable")).when(processor).process(any());

        // when
        listener.onMessage(context);

        // then
        verify(context).abandon();
        verify(context, never()).complete();
        verify(context, never()).deadLetter(any());
    }

    @Test
    void shouldNotThrowWhenReportingServiceBusError() {
        // given
        ServiceBusErrorContext errorContext = mock(ServiceBusErrorContext.class);
        when(errorContext.getException()).thenReturn(new IllegalStateException("connection lost"));

        // when
        // then
        assertThatCode(() -> listener.onError(errorContext)).doesNotThrowAnyException();
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
