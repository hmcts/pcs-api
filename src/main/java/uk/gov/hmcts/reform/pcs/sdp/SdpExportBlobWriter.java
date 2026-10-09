package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;

/**
 * Writes JSON payloads to the SDP export container. Blobs are write-once: an upload to a name that
 * already exists fails with a 409 rather than overwriting what SDP may already have read.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sdp-export", name = "enabled", havingValue = "true")
public class SdpExportBlobWriter {

    static final String CONTENT_TYPE = "application/json";

    private final BlobContainerClient sdpExportBlobContainerClient;
    private final ObjectMapper objectMapper;

    public void write(String blobName, Object payload) {
        byte[] json = toJson(payload);
        BlobClient blobClient = sdpExportBlobContainerClient.getBlobClient(blobName);

        BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromBytes(json))
            .setHeaders(new BlobHttpHeaders().setContentType(CONTENT_TYPE))
            .setRequestConditions(new BlobRequestConditions().setIfNoneMatch("*"));

        blobClient.uploadWithResponse(options, null, Context.NONE);
        log.info("Wrote SDP export blob {}", blobName);
    }

    private byte[] toJson(Object payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialise SDP export payload", e);
        }
    }
}
