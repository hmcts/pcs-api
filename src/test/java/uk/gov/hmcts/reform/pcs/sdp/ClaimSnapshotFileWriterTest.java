package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.Producer;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaimSnapshotFileWriterTest {

    @Test
    void shouldSerialiseSnapshotRecordsAsSnakeCaseJsonArray() throws Exception {
        ObjectMapper objectMapper = new JacksonConfiguration().getMapper();
        ClaimSnapshotFileWriter fileWriter = new ClaimSnapshotFileWriter(objectMapper);
        ClaimSnapshotRecord snapshotRecord = ClaimSnapshotRecord.builder()
            .contractVersion("4.1.0")
            .producer(Producer.builder()
                .system("possessions-ccd")
                .environment("aat")
                .build())
            .extractDate("2026-03-09")
            .snapshotId("snapshot-123")
            .publishedDate("2026-03-09T12:00:00Z")
            .claim(ClaimSnapshot.builder()
                .claimReference("PCS-1001")
                .snapshotVersion(1)
                .recordedDate("2026-03-01 10:00:00")
                .dimensions(SnapshotDimensions.builder()
                    .claimType("AAA3-SLD")
                    .language("eng")
                    .submittedType("Digital")
                    .build())
                .build())
            .build();

        byte[] written = fileWriter.write(List.of(snapshotRecord));
        JsonNode root = objectMapper.readTree(written);

        assertThat(root.isArray()).isTrue();
        assertThat(root).hasSize(1);
        assertThat(root.get(0).get("contract_version").asText()).isEqualTo("4.1.0");
        assertThat(root.get(0).get("producer").get("system").asText()).isEqualTo("possessions-ccd");
        assertThat(root.get(0).get("claim").get("claim_reference").asText()).isEqualTo("PCS-1001");
        assertThat(root.get(0).get("claim").get("dimensions").get("claim_type").asText())
            .isEqualTo("AAA3-SLD");
    }

    @Test
    void shouldThrowWhenSerialiseFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ObjectWriter objectWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsBytes(any())).thenThrow(new JsonProcessingException("boom") {
        });

        assertThatThrownBy(() -> new ClaimSnapshotFileWriter(objectMapper).write(List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to serialise SDP claim snapshot");
    }
}
