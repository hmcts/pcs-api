package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.sdp.SdpExportBlobWriter;
import uk.gov.hmcts.reform.pcs.testingsupport.model.SdpExportRoundTripResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Proves the deployed app can reach the SDP export container with its workload identity: writes a
 * small JSON blob through {@link SdpExportBlobWriter}, reads it back, then deletes it. SDP ingests
 * this container, so test blobs live under {@value #BLOB_PREFIX} and are always deleted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"testing-support.enabled", "sdp-export.enabled"}, havingValue = "true")
public class SdpExportRoundTripService {

    public static final String BLOB_PREFIX = "functional-test/";
    static final String PURPOSE = "pcs-api functional test round trip - not an export, ignore";

    private final SdpExportBlobWriter sdpExportBlobWriter;
    private final BlobContainerClient sdpExportBlobContainerClient;
    private final ObjectMapper objectMapper;

    public SdpExportRoundTripResult roundTrip() {
        String blobName = BLOB_PREFIX + UUID.randomUUID() + ".json";
        BlobClient blobClient = sdpExportBlobContainerClient.getBlobClient(blobName);

        RuntimeException failure = null;
        try {
            writeAndVerify(blobName, blobClient);
        } catch (RuntimeException e) {
            failure = e;
        }

        try {
            blobClient.deleteIfExists();
        } catch (RuntimeException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }

        if (failure != null) {
            log.error("SDP export round trip failed for blob {}", blobName, failure);
            return SdpExportRoundTripResult.failure(blobName, describe(failure));
        }

        log.info("SDP export round trip succeeded for blob {}", blobName);
        return SdpExportRoundTripResult.success(blobName);
    }

    private void writeAndVerify(String blobName, BlobClient blobClient) {
        TestBlob payload = new TestBlob(blobName, PURPOSE);
        sdpExportBlobWriter.write(blobName, payload);

        byte[] downloaded = blobClient.downloadContent().toBytes();
        JsonNode expected = objectMapper.valueToTree(payload);
        JsonNode actual = readTree(downloaded);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Read-back content did not match what was written: " + actual);
        }
    }

    private JsonNode readTree(byte[] json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Read-back content is not valid JSON", e);
        }
    }

    private static String describe(Throwable failure) {
        StringBuilder description = new StringBuilder(failure.getClass().getSimpleName())
            .append(": ").append(failure.getMessage());
        for (Throwable suppressed : failure.getSuppressed()) {
            description.append(" (cleanup also failed: ")
                .append(suppressed.getClass().getSimpleName()).append(": ").append(suppressed.getMessage())
                .append(')');
        }
        return description.toString();
    }

    record TestBlob(String blobName, String purpose) {
    }
}
