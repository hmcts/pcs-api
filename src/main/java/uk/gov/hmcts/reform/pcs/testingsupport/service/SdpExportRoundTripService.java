package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.sdp.SdpExportBlobWriter;
import uk.gov.hmcts.reform.pcs.testingsupport.model.SdpExportRoundTripResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Proves the deployed app can reach the SDP export container with its workload identity: writes a
 * small JSON blob through {@link SdpExportBlobWriter} and reads it back to check the content. The
 * blob is left in place. SDP ingests this container, so test blobs live under {@value #BLOB_PREFIX},
 * are named after the UTC time they were written (to the millisecond, with {@code -} in place of
 * {@code :}, e.g. {@code functional-test/2026-10-08T15-28-21.123Z.json}) and say in their payload
 * that they are functional-test blobs.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = {"testing-support.enabled", "sdp-export.enabled"}, havingValue = "true")
public class SdpExportRoundTripService {

    public static final String BLOB_PREFIX = "functional-test/";
    static final String PURPOSE = "pcs-api functional test round trip - not an export, ignore";

    private static final DateTimeFormatter BLOB_NAME_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter PAYLOAD_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final SdpExportBlobWriter sdpExportBlobWriter;
    private final BlobContainerClient sdpExportBlobContainerClient;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public SdpExportRoundTripService(SdpExportBlobWriter sdpExportBlobWriter,
                                     BlobContainerClient sdpExportBlobContainerClient,
                                     ObjectMapper objectMapper,
                                     @Qualifier("utcClock") Clock utcClock) {
        this.sdpExportBlobWriter = sdpExportBlobWriter;
        this.sdpExportBlobContainerClient = sdpExportBlobContainerClient;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
    }

    public SdpExportRoundTripResult roundTrip() {
        Instant writtenAt = Instant.now(utcClock).truncatedTo(ChronoUnit.MILLIS);
        String blobName = BLOB_PREFIX + BLOB_NAME_TIMESTAMP.format(writtenAt) + ".json";

        try {
            writeAndVerify(blobName, new TestBlob(blobName, PURPOSE, PAYLOAD_TIMESTAMP.format(writtenAt)));
        } catch (RuntimeException e) {
            log.error("SDP export round trip failed for blob {}", blobName, e);
            return SdpExportRoundTripResult.failure(blobName, e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        log.info("SDP export round trip succeeded for blob {}", blobName);
        return SdpExportRoundTripResult.success(blobName);
    }

    private void writeAndVerify(String blobName, TestBlob payload) {
        sdpExportBlobWriter.write(blobName, payload);

        BlobClient blobClient = sdpExportBlobContainerClient.getBlobClient(blobName);
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

    record TestBlob(String blobName, String purpose, String writtenAt) {
    }
}
