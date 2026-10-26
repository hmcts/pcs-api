package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;

import java.util.List;

@Component
public class ClaimSnapshotFileWriter {

    private final ObjectMapper objectMapper;

    public ClaimSnapshotFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] write(List<ClaimSnapshotRecord> records) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(records);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise SDP claim snapshot", e);
        }
    }
}
