package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalUser;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionStart;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.ScheduleEvictionService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleEvictionTest extends BaseEventTest {

    private static final ExternalUser USER = new ExternalUser("user-id", List.of("caseworker-pcs"), null);
    private static final ScheduleEvictionRequest REQUEST = new ScheduleEvictionRequest(
        UUID.randomUUID(), VerticalYesNo.YES, LocalDateTime.of(2027, 6, 25, 10, 0));

    @Mock
    private ScheduleEvictionService scheduleEvictionService;

    @InjectMocks
    private ScheduleEviction underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredAsShowForEvictionSchedulingFeatureFlagAndScheduleEvictionShowCondition() {
        assertConfiguredShowConditions(ShowConditions.and(
            ShowConditions.featureFlagsEnabled(FeatureFlag.EVICTION_SCHEDULING),
            "enfShowConditions.scheduleEviction=\"Yes\""
        ));
    }

    @Test
    void shouldBeAvailableInScheduleEvictionStates() {
        assertConfiguredForStates(EventStates.scheduleEviction());
    }

    @Test
    void shouldGrantBailiffAdmin() {
        assertGrants(UserRole.BAILIFF_ADMIN, Permission.CRU);
    }

    @Test
    void shouldReturnStartDetailsFromService() {
        // Given
        ScheduleEvictionStart startDetails = new ScheduleEvictionStart(
            UUID.randomUUID(), "1234", "1 Street, W3 7RX", "Claimant v Defendant", null,
            List.of("Claimant"), List.of("Defendant"));
        when(scheduleEvictionService.getStartDetails(TEST_CASE_REFERENCE)).thenReturn(startDetails);

        // When
        Object start = ((ExternalStartResponse.Started<?>) configuredEvent.start(payload(), USER, null)).payload();

        // Then
        assertThat(start).isSameAs(startDetails);
    }

    @Test
    void shouldRejectStartWhenServiceRejects() {
        when(scheduleEvictionService.getStartDetails(TEST_CASE_REFERENCE))
            .thenThrow(ExternalRejection.because(ScheduleEvictionService.NO_WARRANT_AWAITING_SCHEDULING));

        assertThat(startRejection()).containsExactly(ScheduleEvictionService.NO_WARRANT_AWAITING_SCHEDULING);
    }

    @Test
    void shouldScheduleEvictionThroughService() {

        SubmitResponse<State> response = submit(REQUEST);

        assertThat(response.getErrors()).isNullOrEmpty();
        verify(scheduleEvictionService).scheduleEviction(TEST_CASE_REFERENCE, REQUEST);
    }

    @Test
    void shouldRejectSubmitWhenServiceRejects() {
        doThrow(ExternalRejection.because(ScheduleEvictionService.DATE_REQUIRED))
            .when(scheduleEvictionService).scheduleEviction(TEST_CASE_REFERENCE, REQUEST);

        assertThat(submit(REQUEST).getErrors()).containsExactly(ScheduleEvictionService.DATE_REQUIRED);
    }

    private EventPayload<PCSCase, State> payload() {
        return new EventPayload<>(TEST_CASE_REFERENCE, null, null);
    }

    private SubmitResponse<State> submit(ScheduleEvictionRequest request) {
        return configuredEvent.submit(payload(), request, USER);
    }

    private List<String> startRejection() {
        return ((ExternalRejection<?>) configuredEvent.start(payload(), USER, null)).errors();
    }
}
