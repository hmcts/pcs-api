package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobUploaderTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Test
    void shouldUploadFileWhenEnabled() {
        Path file = Path.of("/tmp/claim_snapshot_export_2026-10-13.json");
        when(blobContainerClient.getBlobClient(file.getFileName().toString())).thenReturn(blobClient);

        new BlobUploader(blobContainerClient, true).upload(file);

        verify(blobClient).uploadFromFile(file.toString(), true);
    }

    @Test
    void shouldNotUploadFileWhenDisabled() {
        new BlobUploader(blobContainerClient, false)
            .upload(Path.of("/tmp/claim_snapshot_export_2026-10-13.json"));

        verifyNoInteractions(blobContainerClient, blobClient);
    }
}
