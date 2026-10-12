package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes SDP claim snapshot records to a local JSON file.
 */
@Slf4j
@Component
public class FileWriter {

    private final ObjectMapper objectMapper;

    public FileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path write(List<ClaimSnapshotRecord> records, Path outputPath) {
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records);
            Files.writeString(outputPath, json);
            log.info("Wrote {} SDP claim snapshot record(s) to {}", records.size(), outputPath);
            return outputPath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write SDP claim snapshot file to " + outputPath, e);
        }
    }
}
