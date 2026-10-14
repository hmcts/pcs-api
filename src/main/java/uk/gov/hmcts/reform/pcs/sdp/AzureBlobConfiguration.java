package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sdp.export", name = "upload-enabled", havingValue = "true")
public class AzureBlobConfiguration {

    @Bean
    public BlobContainerClient sdpBlobContainerClient(
        @Value("${sdp.export.account-url}") String accountUrl,
        @Value("${sdp.export.container-name}") String containerName) {

        return new BlobContainerClientBuilder()
            .endpoint(accountUrl)
            .containerName(containerName)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }
}
