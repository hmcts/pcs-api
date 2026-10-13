package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
public class BlobUploader {

    private final BlobContainerClient blobContainerClient;
    private final boolean enabled;

    public BlobUploader(BlobContainerClient blobContainerClient,
                        @Value("${sdp.export.upload-enabled}") boolean enabled) {
        this.blobContainerClient = blobContainerClient;
        this.enabled = enabled;
    }

    public void upload(Path file) {
        if (!enabled) {
            return;
        }

        String blobName = file.getFileName().toString();
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            blobClient.uploadFromFile(file.toString(), true);
            log.info("Uploaded SDP claim snapshot file {} to blob {}", file, blobName);
        } catch (RuntimeException e) {
            log.error("Failed to upload SDP claim snapshot file {} to blob {}", file, blobName, e);
            throw e;
        }
    }
}
