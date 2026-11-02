package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobUploaderTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private Response<BlockBlobItem> response;

    @Captor
    private ArgumentCaptor<BlobParallelUploadOptions> uploadOptionsCaptor;

    @Test
    void shouldUploadJsonFromFile() throws Exception {
        String blobName = "claim_snapshot_2026-10-13.json";
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.uploadWithResponse(any(), isNull(), eq(Context.NONE))).thenReturn(response);
        when(response.getStatusCode()).thenReturn(201);
        Path file = snapshotFile();

        try {
            new BlobUploader(blobContainerClient).upload(blobName, file);
            verify(blobClient).uploadWithResponse(uploadOptionsCaptor.capture(), isNull(), eq(Context.NONE));
            BlobParallelUploadOptions options = uploadOptionsCaptor.getValue();
            assertThat(options.getHeaders().getContentType()).isEqualTo("application/json");
            assertThat(new String(BinaryData.fromFlux(options.getDataFlux()).block().toBytes(), StandardCharsets.UTF_8))
                .isEqualTo("{\"ok\":true}");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldRethrowWhenUploadFails() throws Exception {
        String blobName = "claim_snapshot_2026-10-13.json";
        Path file = snapshotFile();
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.uploadWithResponse(any(), isNull(), eq(Context.NONE)))
            .thenThrow(new IllegalStateException("upload failed"));

        try {
            assertThatThrownBy(() -> new BlobUploader(blobContainerClient).upload(blobName, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("upload failed");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldThrowWhenStatusIsNotSuccess() throws Exception {
        String blobName = "claim_snapshot_2026-10-13.json";
        Path file = snapshotFile();
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.uploadWithResponse(any(), isNull(), eq(Context.NONE))).thenReturn(response);
        when(response.getStatusCode()).thenReturn(500);

        try {
            assertThatThrownBy(() -> new BlobUploader(blobContainerClient).upload(blobName, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to upload SDP claim snapshot to blob " + blobName + " status=500");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static Path snapshotFile() throws Exception {
        Path file = Files.createTempFile("sdp-claim-snapshot-", ".json");
        Files.writeString(file, "{\"ok\":true}");
        return file;
    }
}
