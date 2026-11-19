package uk.gov.hmcts.reform.pcs.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownstreamHealthIndicatorsTest {

    @Mock
    private PaymentsHealthApi paymentsHealthApi;

    @Mock
    private SendLetterHealthApi sendLetterHealthApi;

    @Test
    void shouldReadStatusFromHealthEndpointJson() throws Exception {
        DownstreamHealth health = new ObjectMapper()
            .readValue("{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"}}}", DownstreamHealth.class);

        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.toHealth().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void shouldReportPaymentsStatus() {
        when(paymentsHealthApi.health()).thenReturn(new DownstreamHealth("DOWN"));

        assertThat(new PaymentsHealthIndicator(paymentsHealthApi).health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void shouldReportPaymentsDownWhenHealthCallFails() {
        when(paymentsHealthApi.health()).thenThrow(new IllegalStateException("unreachable"));

        Health health = new PaymentsHealthIndicator(paymentsHealthApi).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "java.lang.IllegalStateException: unreachable");
    }

    @Test
    void shouldReportSendLetterStatus() {
        when(sendLetterHealthApi.health()).thenReturn(new DownstreamHealth("UP"));

        assertThat(new SendLetterHealthIndicator(sendLetterHealthApi).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void shouldReportSendLetterDownWhenHealthCallFails() {
        when(sendLetterHealthApi.health()).thenThrow(new IllegalStateException("unreachable"));

        assertThat(new SendLetterHealthIndicator(sendLetterHealthApi).health().getStatus()).isEqualTo(Status.DOWN);
    }
}
