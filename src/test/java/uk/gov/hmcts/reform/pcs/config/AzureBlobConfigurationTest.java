package uk.gov.hmcts.reform.pcs.config;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AzureBlobConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AzureBlobConfiguration.class)
        .withPropertyValues(
            "sdp.export.account-url=https://example.blob.core.windows.net",
            "sdp.export.container-name=sdp-export"
        );

    @Test
    void shouldNotLoadWhenUploadDisabled() {
        contextRunner
            .withPropertyValues("sdp.export.upload-enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(BlobContainerClient.class));
    }

    @Test
    void shouldLoadWhenUploadEnabled() {
        contextRunner
            .withPropertyValues("sdp.export.upload-enabled=true")
            .run(context -> assertThat(context).hasSingleBean(BlobContainerClient.class));
    }
}
