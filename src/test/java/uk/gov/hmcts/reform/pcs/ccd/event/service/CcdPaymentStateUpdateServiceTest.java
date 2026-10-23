package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CcdPaymentStateUpdateTaskData;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforcementPayment;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTaskComponent.CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CcdPaymentStateUpdateServiceTest {

    private static final long CASE_ID = 1234L;
    private static final String IDAM_BEARER = "Bearer idam";
    private static final String S2S_BEARER = "Bearer s2s";

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AuthTokenGenerator s2sAuthTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SchedulerClient schedulerClient;
    @Captor
    private ArgumentCaptor<SchedulableInstance<CcdPaymentStateUpdateTaskData>> schedulableInstanceCaptor;

    @InjectMocks
    private CcdPaymentStateUpdateService underTest;

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldScheduleCcdPaymentUpdateTask(EventId eventId) {
        // Given
        Instant before = Instant.now();

        // When
        underTest.submitPaymentSuccess(CASE_ID, eventId);

        // Then
        Instant after = Instant.now();
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<CcdPaymentStateUpdateTaskData> scheduled = schedulableInstanceCaptor.getValue();
        TaskInstance<CcdPaymentStateUpdateTaskData> taskInstance = scheduled.getTaskInstance();

        assertThat(taskInstance.getTaskName()).isEqualTo(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR.getTaskName());
        assertThat(taskInstance.getId()).isEqualTo(eventId + "-" + CASE_ID);
        assertThat(taskInstance.getData().getCaseReference()).isEqualTo(CASE_ID);
        assertThat(taskInstance.getData().getEventId()).isEqualTo(eventId);
        assertThat(scheduled.getNextExecutionTime(Instant.now())).isBetween(before, after);
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldStartEventAndSubmitPaymentSuccessfully(EventId eventId) {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, eventId);

        // Then
        verify(schedulerClient).scheduleIfNotExists(any());
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldUseSameInstanceIdWhenSubmittedMultipleTimesForSamePayment(EventId eventId) {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, eventId);
        underTest.submitPaymentSuccess(CASE_ID, eventId);

        // Then
        verify(schedulerClient, times(2)).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        List<String> instanceIds = schedulableInstanceCaptor.getAllValues().stream()
            .map(scheduled -> scheduled.getTaskInstance().getId())
            .toList();

        assertThat(instanceIds).containsExactly(
            eventId.name() + "-" + CASE_ID,
            eventId.name() + "-" + CASE_ID
        );
    }

    @Test
    void shouldUseDifferentInstanceIdsForDifferentEvents() {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, claimIssuePayment);
        underTest.submitPaymentSuccess(CASE_ID, enforcementPayment);

        // Then
        verify(schedulerClient, times(2)).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        var instanceIds = schedulableInstanceCaptor.getAllValues().stream()
            .map(scheduled -> scheduled.getTaskInstance().getId())
            .toList();

        assertThat(instanceIds).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldUpdateCcd(EventId eventId) {
        // Given
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_BEARER);
        when(s2sAuthTokenGenerator.generate()).thenReturn(S2S_BEARER);
        StartEventResponse startEventResponse = StartEventResponse.builder().token(IDAM_BEARER).build();
        when(coreCaseDataApi.startEvent(IDAM_BEARER, S2S_BEARER, String.valueOf(CASE_ID), eventId.name()))
            .thenReturn(startEventResponse);

        CaseResource expectedCaseResource = new CaseResource();
        when(coreCaseDataApi.createEvent(eq(IDAM_BEARER), eq(S2S_BEARER), eq(String.valueOf(CASE_ID)),
                                         any(CaseDataContent.class)))
            .thenReturn(expectedCaseResource);
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));
        CcdPaymentStateUpdateTaskData taskData = CcdPaymentStateUpdateTaskData.builder().eventId(eventId)
            .caseReference(CASE_ID).build();

        // When
        underTest.updateCcd(taskData);

        // Then
        verify(coreCaseDataApi).startEvent(IDAM_BEARER, S2S_BEARER, String.valueOf(CASE_ID), eventId.name());
        ArgumentCaptor<CaseDataContent> contentCaptor = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).createEvent(eq(IDAM_BEARER), eq(S2S_BEARER), eq(String.valueOf(CASE_ID)),
                                            contentCaptor.capture());
        CaseDataContent submitted = contentCaptor.getValue();
        assertThat(submitted.getEventToken()).isEqualTo(IDAM_BEARER);
        assertThat(submitted.getEvent().getId()).isEqualTo(eventId.name());
    }

    private static Stream<Arguments> eventIds() {
        return Stream.of(
            argumentSet(claimIssuePayment.name(), claimIssuePayment),
            argumentSet(enforcementPayment.name(), enforcementPayment)
        );
    }

}
