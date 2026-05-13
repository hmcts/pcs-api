package uk.gov.hmcts.reform.pcs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.authorisation.ServiceAuthAutoConfiguration;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.config.ServiceTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.sendletter.SendLetterAutoConfiguration;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.config.RetryConfig;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    FeignAutoConfiguration.class,
    SendLetterAutoConfiguration.class,
    RetryConfig.class,
    ServiceAuthAutoConfiguration.class,
    ServiceTokenGeneratorConfiguration.class
})
@TestPropertySource(properties = {
    "send-letter.url=http://rpe-send-letter-service-demo.service.core-compute-demo.internal",
    "idam.s2s-auth.url=http://rpe-service-auth-provider-demo.service.core-compute-demo.internal",
    "idam.s2s-auth.microservice=pcs_api",
    "idam.s2s-auth.totp_secret=HMLBO7EEZQ3RTDKD"
})
class BulkPrintPocTest {

    @Autowired
    private AuthTokenGenerator s2sTokenGenerator;

    @Autowired
    private SendLetterApi sendLetterApi;

    private String s2sToken;

    @BeforeEach
    void setup() {
        s2sToken = s2sTokenGenerator.generate();
    }

    @Test
    void sendToBulkPrint() throws IOException {
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-document.pdf"));
        String encodedPdf = Base64.getEncoder().encodeToString(pdfBytes);
        Map<String, Object> additionalData = Map.of(
            "recipients", List.of("John Smith"),
            "isInternational", false
        );

        LetterWithPdfsRequest letter =
            new LetterWithPdfsRequest(List.of(encodedPdf), "CPD-01-IN1", additionalData);

        sendLetterApi.sendLetter(s2sToken, letter);
    }
}
