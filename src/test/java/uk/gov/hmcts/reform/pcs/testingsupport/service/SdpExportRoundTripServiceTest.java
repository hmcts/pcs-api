package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.sdp.SdpExportBlobWriter;
import uk.gov.hmcts.reform.pcs.testingsupport.model.SdpExportRoundTripResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdpExportRoundTripServiceTest {

    @Mock
    private SdpExportBlobWriter sdpExportBlobWriter;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Captor
    private ArgumentCaptor<String> blobNameCaptor;

    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    private SdpExportRoundTripService underTest;

    @BeforeEach
    void setUp() {
        underTest = new SdpExportRoundTripService(sdpExportBlobWriter, blobContainerClient, objectMapper);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
    }

    @Test
    void shouldWriteReadBackAndDeleteATestBlob() {
        // Given
        echoWrittenPayloadOnDownload();

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        String blobName = blobNameCaptor.getValue();
        assertThat(blobName).startsWith("functional-test/").endsWith(".json");
        assertThat(result).isEqualTo(SdpExportRoundTripResult.success(blobName));
        verify(sdpExportBlobWriter).write(
            blobName, new SdpExportRoundTripService.TestBlob(blobName, SdpExportRoundTripService.PURPOSE));
        verify(blobClient).deleteIfExists();
    }

    @Test
    void shouldFailAndStillDeleteWhenReadBackDiffers() {
        // Given
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString("{\"blobName\":\"other\"}"));

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result.roundTrip()).isFalse();
        assertThat(result.blobName()).isEqualTo(blobNameCaptor.getValue());
        assertThat(result.error()).startsWith("IllegalStateException: Read-back content did not match");
        verify(blobClient).deleteIfExists();
    }

    @Test
    void shouldReportTheWriteFailureAndNotReadBack() {
        // Given
        doThrow(new IllegalStateException("Status code 403, AuthorizationPermissionMismatch"))
            .when(sdpExportBlobWriter).write(anyString(), any());

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result.roundTrip()).isFalse();
        assertThat(result.error()).isEqualTo("IllegalStateException: Status code 403, AuthorizationPermissionMismatch");
        verify(blobClient, never()).downloadContent();
        verify(blobClient).deleteIfExists();
    }

    @Test
    void shouldKeepTheOriginalFailureWhenCleanupAlsoFails() {
        // Given
        doThrow(new IllegalStateException("write failed"))
            .when(sdpExportBlobWriter).write(anyString(), any());
        when(blobClient.deleteIfExists()).thenThrow(new IllegalStateException("delete failed"));

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result.error()).isEqualTo(
            "IllegalStateException: write failed (cleanup also failed: IllegalStateException: delete failed)");
    }

    @Test
    void shouldFailWhenOnlyTheDeleteFails() {
        // Given
        echoWrittenPayloadOnDownload();
        when(blobClient.deleteIfExists()).thenThrow(new IllegalStateException("delete failed"));

        // When
        SdpExportRoundTripResult result = underTest.roundTrip();

        // Then
        assertThat(result.roundTrip()).isFalse();
        assertThat(result.error()).isEqualTo("IllegalStateException: delete failed");
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
