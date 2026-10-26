package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.Producer;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimSnapshotFileWriterTest {

    @TempDir
    private Path tempDir;

    private ObjectMapper objectMapper;
    private ClaimSnapshotFileWriter fileWriter;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfiguration().getMapper();
        fileWriter = new ClaimSnapshotFileWriter(objectMapper);
    }

    @Test
    void shouldWriteSnapshotRecordsAsSnakeCaseJsonArray() throws Exception {
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

        Path outputPath = tempDir.resolve("claim_snapshot_export.json");

        byte[] written = fileWriter.write(List.of(snapshotRecord), outputPath);

        assertThat(outputPath).exists();
        assertThat(written).isEqualTo(Files.readAllBytes(outputPath));
        String json = Files.readString(outputPath);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.isArray()).isTrue();
        assertThat(root).hasSize(1);
        assertThat(root.get(0).get("contract_version").asText()).isEqualTo("4.1.0");
        assertThat(root.get(0).get("producer").get("system").asText()).isEqualTo("possessions-ccd");
        assertThat(root.get(0).get("claim").get("claim_reference").asText()).isEqualTo("PCS-1001");
        assertThat(root.get(0).get("claim").get("dimensions").get("claim_type").asText())
            .isEqualTo("AAA3-SLD");
    }

    @Test
    void shouldThrowWhenFileCannotBeWritten() throws Exception {
        Path blocked = tempDir.resolve("blocked");
        Files.writeString(blocked, "not-a-directory");
        Path outputPath = blocked.resolve("claim_snapshot_export.json");

        assertThatThrownBy(() -> fileWriter.write(List.of(), outputPath))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to write SDP claim snapshot file to " + outputPath);
    }
}
