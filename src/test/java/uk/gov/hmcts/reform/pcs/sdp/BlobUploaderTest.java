package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobUploaderTest {

    @TempDir
    private Path tempDir;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Captor
    private ArgumentCaptor<BlobParallelUploadOptions> uploadOptionsCaptor;

    @Test
    void shouldUploadJsonWithoutOverwriting() throws Exception {
        Path file = tempDir.resolve("claim_snapshot_export_2026-10-13.json");
        Files.writeString(file, "{\"ok\":true}");
        when(blobContainerClient.getBlobClient(file.getFileName().toString())).thenReturn(blobClient);

        new BlobUploader(blobContainerClient).upload(file);

        verify(blobClient).uploadWithResponse(uploadOptionsCaptor.capture(), isNull(), eq(Context.NONE));
        BlobParallelUploadOptions options = uploadOptionsCaptor.getValue();
        assertThat(options.getHeaders().getContentType()).isEqualTo("application/json");
        assertThat(options.getRequestConditions().getIfNoneMatch()).isEqualTo("*");
        assertThat(new String(BinaryData.fromFlux(options.getDataFlux()).block().toBytes(), StandardCharsets.UTF_8))
            .isEqualTo("{\"ok\":true}");
    }
}
