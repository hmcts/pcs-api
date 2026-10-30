package uk.gov.hmcts.reform.pcs.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.dmstore.wiremock.AddDocumentIdHeaderTransformer;
import uk.gov.hmcts.reform.pcs.dmstore.wiremock.SaveDocumentFileTransformer;
import uk.gov.hmcts.reform.pcs.dmstore.wiremock.ServeDocumentFileTransformer;
import uk.gov.hmcts.reform.pcs.docmosis.wiremock.GenerateDocumentTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Slf4j
public class WiremockConfiguration {

    private static final int WIREMOCK_PORT = 9400;

    @Bean
    public Path wiremockWorkingDirectory() throws IOException {
        Path wiremockWorkingDirectory = Files.createTempDirectory("cftlibtests").toAbsolutePath();
        log.info("Wiremock working directory is " + wiremockWorkingDirectory);
        return wiremockWorkingDirectory;
    }

    @Bean(destroyMethod = "stop")
    public WireMockServer wireMockServer(GenerateDocumentTransformer generateDocumentTransformer,
                                         SaveDocumentFileTransformer saveDocumentFileTransformer,
                                         @Qualifier("wiremockWorkingDirectory") Path wiremockWorkingDirectory) {

        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(WIREMOCK_PORT)
                .usingFilesUnderDirectory(wiremockWorkingDirectory.toString())
                .extensions(
                    generateDocumentTransformer,
                    saveDocumentFileTransformer,
                    new ServeDocumentFileTransformer(),
                    new AddDocumentIdHeaderTransformer()
                )
        );

        wireMockServer.start();

        wireMockServer.resetAll();

        return wireMockServer;
    }

}
