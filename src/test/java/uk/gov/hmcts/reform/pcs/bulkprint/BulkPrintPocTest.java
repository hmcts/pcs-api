package uk.gov.hmcts.reform.pcs.bulkprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.authorisation.ServiceAuthAutoConfiguration;
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
    "send-letter.url=http://rpe-send-letter-service-demo.service.core-compute-demo.internal"
})
class BulkPrintPocTest {

    @Autowired
    private SendLetterApi sendLetterApi;

    private String s2sToken;

    @BeforeEach
    void setup() {
        ServiceAuthenticationGenerator s2sAuthTokenGenerator = new ServiceAuthenticationGenerator();
        String s2sToken = s2sAuthTokenGenerator.generate();
        this.s2sToken = "Bearer " + s2sToken;
    }

    @ParameterizedTest
    @CsvSource({
        "Al Bloggs, CPD-01-IN1, false",
        "Bob Bloggs, CPC-01-IN0, true",
        "Cam Bloggs, DEF-01-IN0, false",
        "Dave Bloggs, GEN-00-IN0, true"
    })
    void sendToBulkPrint(String name, String letterType, boolean isInternational) throws IOException {
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-document.pdf"));
        String encodedPdf = Base64.getEncoder().encodeToString(pdfBytes);

        Map<String, Object> additionalData = Map.of(
            "recipients", List.of(name),
            "isInternational", isInternational
        );

        LetterWithPdfsRequest letter =
            new LetterWithPdfsRequest(List.of(encodedPdf), letterType, additionalData);

        System.out.println(sendLetterApi.sendLetter(s2sToken, letter).letterId);
    }
}
