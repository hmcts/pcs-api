package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.sdp.SdpExportBlobWriter;
import uk.gov.hmcts.reform.pcs.testingsupport.model.SdpExportRoundTripResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdpExportRoundTripServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T15:28:21.123456Z");
    private static final String EXPECTED_BLOB_NAME = "functional-test/2026-10-08T15-28-21.123Z.json";

    @Mock
    private SdpExportBlobWriter sdpExportBlobWriter;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    @Test
    void shouldWriteAndReadBackATimestampedTestBlob() {
        // Given
        SdpExportRoundTripService underTest = serviceAt(NOW);
        when(blobContainerClient.getBlobClient(EXPECTED_BLOB_NAME)).thenReturn(blobClient);
        echoWrittenPayloadOnDownload();

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result).isEqualTo(SdpExportRoundTripResult.success(EXPECTED_BLOB_NAME));
        verify(sdpExportBlobWriter).write(
            EXPECTED_BLOB_NAME,
            new SdpExportRoundTripService.TestBlob(
                EXPECTED_BLOB_NAME, SdpExportRoundTripService.PURPOSE, "2026-10-08T15:28:21.123Z"));
    }

    @Test
    void shouldAlwaysIncludeMillisecondsInTheBlobName() {
        // Given
        SdpExportRoundTripService underTest = serviceAt(Instant.parse("2026-10-08T15:28:21Z"));
        String blobName = "functional-test/2026-10-08T15-28-21.000Z.json";
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        echoWrittenPayloadOnDownload();

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result).isEqualTo(SdpExportRoundTripResult.success(blobName));
    }

    @Test
    void shouldFailWhenReadBackDiffers() {
        // Given
        SdpExportRoundTripService underTest = serviceAt(NOW);
        when(blobContainerClient.getBlobClient(EXPECTED_BLOB_NAME)).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString("{\"blobName\":\"other\"}"));

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result.roundTrip()).isFalse();
        assertThat(result.blobName()).isEqualTo(EXPECTED_BLOB_NAME);
        assertThat(result.error()).startsWith("IllegalStateException: Read-back content did not match");
    }

    @Test
    void shouldReportTheWriteFailureAndNotReadBack() {
        // Given
        SdpExportRoundTripService underTest = serviceAt(NOW);
        doThrow(new IllegalStateException("Status code 409, BlobAlreadyExists"))
            .when(sdpExportBlobWriter).write(anyString(), any());

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result).isEqualTo(SdpExportRoundTripResult.failure(
            EXPECTED_BLOB_NAME, "IllegalStateException: Status code 409, BlobAlreadyExists"));
        verifyNoInteractions(blobContainerClient);
    }

    private SdpExportRoundTripService serviceAt(Instant instant) {
        return new SdpExportRoundTripService(
            sdpExportBlobWriter, blobContainerClient, objectMapper, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void echoWrittenPayloadOnDownload() {
        BinaryData[] written = new BinaryData[1];
        doAnswer(invocation -> {
            written[0] = BinaryData.fromBytes(objectMapper.writeValueAsBytes(invocation.getArgument(1)));
            return null;
        }).when(sdpExportBlobWriter).write(anyString(), any());
        when(blobClient.downloadContent()).thenAnswer(invocation -> written[0]);
    }
}
