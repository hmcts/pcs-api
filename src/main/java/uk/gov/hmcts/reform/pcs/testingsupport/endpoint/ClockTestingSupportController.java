package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.config.ClockConfiguration;
import uk.gov.hmcts.reform.pcs.testingsupport.clock.TestClockOffset;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Test-only control of the application's clock, so that the end to end tests can put a date and time in the
 * past or the future instead of quoting one and hoping. Every business rule measured against "now" reads it
 * through the {@code ukClock} or {@code utcClock} bean, so moving the offset moves all of them: the eviction
 * date must be today or in the future, the served date and the recorded eviction cannot be in the future, the
 * claimant's 72 hour confirmation deadline, and the 12 calendar month free repeat warrant period.
 *
 * <p>The offset is process wide: it moves the clock for every case and every user on the stack, not just the
 * caller's, so scenarios that set it cannot run alongside scenarios that depend on the real date.
 *
 * <p>It does not move db-scheduler, which decides when a task is due against the real system clock. Moving
 * the clock backwards is therefore always safe; moving it forwards leaves any task queued while it is set
 * waiting until the real time catches up.
 *
 * <p>There is no {@link TestClockOffset} bean unless testing support is enabled, so on an environment with
 * testing support off this controller is absent and the clocks are the real ones.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/testing-support/clock")
@ConditionalOnProperty(name = "testing-support.clock.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class ClockTestingSupportController {

    private final TestClockOffset testClockOffset;

    @Operation(summary = "Read the time the application's clocks are reporting")
    @GetMapping
    public ClockState getClock(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization) {

        return clockState();
    }

    /**
     * Moves the clocks to an instant, to a UK local date and time, or by an offset from the real time.
     * Exactly one of the three has to be given.
     */
    @Operation(summary = "Move the application's clocks to a given time, or by an offset from the real time")
    @PostMapping
    public ResponseEntity<ClockState> setClock(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestBody SetClockRequest request) {

        if (request.given() != 1) {
            return ResponseEntity.badRequest().build();
        }
        if (request.offset() != null) {
            testClockOffset.shiftBy(request.offset());
        } else {
            testClockOffset.setTo(request.instant() != null
                                      ? request.instant()
                                      : request.dateTime().atZone(ClockConfiguration.UK_ZONE_ID).toInstant());
        }
        ClockState state = clockState();
        log.warn("Testing support: the application's clocks now report {} ({} from the real time)",
                 state.now(), state.offset());
        return ResponseEntity.ok(state);
    }

    @Operation(summary = "Put the application's clocks back on the real time")
    @DeleteMapping
    public ClockState resetClock(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization) {

        testClockOffset.reset();
        log.info("Testing support: the application's clocks are back on the real time");
        return clockState();
    }

    private ClockState clockState() {
        Instant now = testClockOffset.now();
        return new ClockState(now, LocalDateTime.ofInstant(now, ClockConfiguration.UK_ZONE_ID),
                              testClockOffset.getOffset());
    }

    /**
     * Where to move the clocks to: an instant ({@code 2026-07-08T09:00:00Z}), a UK local date and time
     * ({@code 2026-07-08T10:00:00}), or an offset from the real time ({@code PT73H}, {@code -PT24H}).
     */
    public record SetClockRequest(Instant instant, LocalDateTime dateTime, Duration offset) {

        int given() {
            return (instant == null ? 0 : 1) + (dateTime == null ? 0 : 1) + (offset == null ? 0 : 1);
        }
    }

    /**
     * What the clocks now report: the instant, the same moment as a UK local date and time, and how far that
     * is from the real time. The offset is written as an ISO-8601 duration, the way the request gives it,
     * rather than as the seconds Jackson defaults to.
     */
    public record ClockState(
        Instant now,
        LocalDateTime ukDateTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Duration offset) {
    }
}
