package uk.gov.hmcts.reform.pcs.sdp;

/*
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sdp.export", name = "upload-enabled", havingValue = "true")
public class BlobUploader {

    static final String CONTENT_TYPE = "application/json";

    private final BlobContainerClient blobContainerClient;

    public BlobUploader(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    public void upload(Path file) {
        String blobName = file.getFileName().toString();
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromFile(file))
                .setHeaders(new BlobHttpHeaders().setContentType(CONTENT_TYPE))
                .setRequestConditions(new BlobRequestConditions().setIfNoneMatch("*"));
            blobClient.uploadWithResponse(options, null, Context.NONE);
            log.info("Uploaded SDP claim snapshot file {} to blob {}", file, blobName);
        } catch (RuntimeException e) {
            log.error("Failed to upload SDP claim snapshot file {} to blob {}", file, blobName, e);
            throw e;
        }
    }
}
*/
