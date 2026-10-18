package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.testingsupport.clock.TestClockOffset;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * The clock the end to end tests set (`support/test-clock.ts`), over HTTP: the request bodies the suite sends
 * and the effect on the {@code ukClock} bean every business rule reads.
 */
@SpringBootTest(properties = "testing-support.clock.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("Testing support clock")
class ClockTestingSupportIT extends AbstractPostgresContainerIT {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String S2S_TOKEN = "Bearer s2s";

    /** 10:00 on a July morning is UK summer time, so 09:00 UTC. */
    private static final LocalDateTime JULY_MORNING = LocalDateTime.parse("2026-07-08T10:00:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClockOffset testClockOffset;

    @Autowired
    @Qualifier("ukClock")
    private Clock ukClock;

    @AfterEach
    void putTheClockBack() {
        testClockOffset.reset();
    }

    @Test
    @DisplayName("moves the UK clock to a UK local date and time")
    void movesTheClockToAUkDateAndTime() throws Exception {
        mockMvc.perform(post("/testing-support/clock")
                            .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                            .contentType(APPLICATION_JSON)
                            .content("{\"dateTime\":\"2026-07-08T10:00:00\"}"))
            .andExpect(status().isOk())
            // Reading the real time to work out the offset and reading it back take a few microseconds.
            .andExpect(jsonPath("$.now", startsWith("2026-07-08T09:00:00")))
            .andExpect(jsonPath("$.ukDateTime", startsWith("2026-07-08T10:00:00")));

        assertThat(LocalDateTime.now(ukClock)).isCloseTo(JULY_MORNING, within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("moves the UK clock to an instant")
    void movesTheClockToAnInstant() throws Exception {
        mockMvc.perform(post("/testing-support/clock")
                            .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                            .contentType(APPLICATION_JSON)
                            .content("{\"instant\":\"2026-07-08T09:00:00Z\"}"))
            .andExpect(status().isOk());

        assertThat(ukClock.instant()).isCloseTo(Instant.parse("2026-07-08T09:00:00Z"),
                                                within(5, ChronoUnit.SECONDS));
        assertThat(ukClock.getZone()).isEqualTo(UK_ZONE_ID);
    }

    @Test
    @DisplayName("moves the UK clock by an offset from the real time")
    void movesTheClockByAnOffset() throws Exception {
        mockMvc.perform(post("/testing-support/clock")
                            .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                            .contentType(APPLICATION_JSON)
                            .content("{\"offset\":\"-PT73H\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offset", is("PT-73H")));

        assertThat(ukClock.instant()).isBefore(Instant.now().minus(Duration.ofHours(72)));
    }

    @Test
    @DisplayName("refuses a request that names no time")
    void refusesARequestThatNamesNoTime() throws Exception {
        mockMvc.perform(post("/testing-support/clock")
                            .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
            .andExpect(status().isBadRequest());

        assertThat(testClockOffset.getOffset()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("puts the UK clock back on the real time")
    void putsTheClockBackOnTheRealTime() throws Exception {
        testClockOffset.setTo(Instant.parse("2026-07-08T09:00:00Z"));

        mockMvc.perform(delete("/testing-support/clock").header(SERVICE_AUTHORIZATION, S2S_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offset", is("PT0S")));

        assertThat(ukClock.instant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }
}
