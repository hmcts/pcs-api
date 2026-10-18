package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.testingsupport.clock.TestClockOffset;
import uk.gov.hmcts.reform.pcs.testingsupport.endpoint.ClockTestingSupportController.ClockState;
import uk.gov.hmcts.reform.pcs.testingsupport.endpoint.ClockTestingSupportController.SetClockRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClockTestingSupportControllerTest {

    private static final String S2S_TOKEN = "s2s";
    /** 10:00 on a July morning is UK summer time, so 09:00 UTC. */
    private static final LocalDateTime JULY_MORNING = LocalDateTime.parse("2026-07-08T10:00:00");
    private static final Instant JULY_MORNING_UTC = Instant.parse("2026-07-08T09:00:00Z");
    private static final TemporalUnitOffset TOLERANCE = within(5, ChronoUnit.SECONDS);

    private final TestClockOffset testClockOffset = new TestClockOffset();
    private final ClockTestingSupportController underTest = new ClockTestingSupportController(testClockOffset);

    @Test
    void shouldReportTheRealTimeBeforeTheClockIsMoved() {
        ClockState state = underTest.getClock(S2S_TOKEN);

        assertThat(state.offset()).isEqualTo(Duration.ZERO);
        assertThat(state.now()).isCloseTo(Instant.now(), TOLERANCE);
    }

    @Test
    void shouldMoveTheClockToAUkDateAndTime() {
        ResponseEntity<ClockState> response =
            underTest.setClock(S2S_TOKEN, new SetClockRequest(null, JULY_MORNING, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().now()).isCloseTo(JULY_MORNING_UTC, TOLERANCE);
        assertThat(response.getBody().ukDateTime()).isCloseTo(JULY_MORNING, TOLERANCE);
        assertThat(testClockOffset.now()).isCloseTo(JULY_MORNING_UTC, TOLERANCE);
    }

    @Test
    void shouldMoveTheClockToAnInstant() {
        ResponseEntity<ClockState> response =
            underTest.setClock(S2S_TOKEN, new SetClockRequest(JULY_MORNING_UTC, null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testClockOffset.now()).isCloseTo(JULY_MORNING_UTC, TOLERANCE);
    }

    @Test
    void shouldMoveTheClockByAnOffset() {
        ResponseEntity<ClockState> response =
            underTest.setClock(S2S_TOKEN, new SetClockRequest(null, null, Duration.ofHours(-73)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().offset()).isEqualTo(Duration.ofHours(-73));
        assertThat(testClockOffset.now()).isBefore(Instant.now().minus(Duration.ofHours(72)));
    }

    @Test
    void shouldRefuseARequestThatNamesNoTime() {
        ResponseEntity<ClockState> response =
            underTest.setClock(S2S_TOKEN, new SetClockRequest(null, null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(testClockOffset.getOffset()).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldRefuseARequestThatNamesMoreThanOneTime() {
        ResponseEntity<ClockState> response =
            underTest.setClock(S2S_TOKEN, new SetClockRequest(JULY_MORNING_UTC, JULY_MORNING, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(testClockOffset.getOffset()).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldPutTheClockBackOnTheRealTime() {
        underTest.setClock(S2S_TOKEN, new SetClockRequest(JULY_MORNING_UTC, null, null));

        ClockState state = underTest.resetClock(S2S_TOKEN);

        assertThat(state.offset()).isEqualTo(Duration.ZERO);
        assertThat(state.now()).isCloseTo(Instant.now(), TOLERANCE);
        assertThat(testClockOffset.getOffset()).isEqualTo(Duration.ZERO);
    }
}
