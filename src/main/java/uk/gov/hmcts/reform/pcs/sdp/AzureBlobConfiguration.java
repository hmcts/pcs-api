package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfiguration {

    @Bean
    public BlobContainerClient sdpBlobContainerClient(
        @Value("${sdp.export.storage-endpoint}") String storageEndpoint,
        @Value("${sdp.export.container-name}") String containerName) {

        return new BlobContainerClientBuilder()
            .endpoint(storageEndpoint)
            .containerName(containerName)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }
}
