package uk.gov.hmcts.reform.pcs.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Blob container the SDP team reads exports from. Authenticates with the pod's workload identity
 * (AZURE_CLIENT_ID / AZURE_TENANT_ID / AZURE_FEDERATED_TOKEN_FILE), so no keys or connection strings.
 */
@Configuration
@ConditionalOnProperty(prefix = "sdp-export", name = "enabled", havingValue = "true")
public class SdpExportStorageConfiguration {

    @Bean
    public BlobContainerClient sdpExportBlobContainerClient(
        @Value("${sdp-export.account-url}") String accountUrl,
        @Value("${sdp-export.container}") String container
    ) {
        return new BlobContainerClientBuilder()
            .endpoint(accountUrl)
            .containerName(container)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }

}
