package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sdp.export", name = "upload-enabled", havingValue = "true")
public class BlobUploader {

    static final String CONTENT_TYPE = "application/json";

    private final BlobContainerClient blobContainerClient;

    public BlobUploader(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    public void upload(String blobName, byte[] json) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromBytes(json))
                .setHeaders(new BlobHttpHeaders().setContentType(CONTENT_TYPE));
            Response<?> response = blobClient.uploadWithResponse(options, null, Context.NONE);
            log.info("Uploaded SDP claim snapshot to blob {} status={}", blobName, response.getStatusCode());
            
            int status = response.getStatusCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException(
                    "Failed to upload SDP claim snapshot to blob " + blobName
                        + " status=" + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            log.error("Failed to upload SDP claim snapshot to blob {}", blobName, e);
            throw e;
        }
    }
}
