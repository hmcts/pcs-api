package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class ClaimSnapshotFileWriter {

    private final ObjectMapper objectMapper;

    public ClaimSnapshotFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonGenerator openArray(OutputStream out) throws IOException {
        JsonGenerator generator = objectMapper.getFactory().createGenerator(out);
        generator.writeStartArray();
        return generator;
    }

    void writeRecord(JsonGenerator generator, ClaimSnapshotRecord record) throws IOException {
        objectMapper.writeValue(generator, record);
    }

    void finishArray(JsonGenerator generator) throws IOException {
        generator.writeEndArray();
        generator.flush();
    }
}
