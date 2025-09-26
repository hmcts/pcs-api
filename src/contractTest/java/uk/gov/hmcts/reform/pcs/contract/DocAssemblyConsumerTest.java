package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.pcs.document.model.JsonNodeFormPayload;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration({FeignAutoConfiguration.class, FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class})
@EnableFeignClients(clients = DocAssemblyClient.class)
@TestPropertySource(properties = "doc-assembly.url=http://dg-docassembly-aat.service.core-compute-aat.internal")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "doc_assembly_template_rendition_provider", port = "80")

public class DocAssemblyConsumerTest {

    private static final String TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";
    private static final String CCD_CASE_REFERENCE = "1234567891011121";
    private static final String AUTHORIZATION_HEADER = "Bearer userToken";
    private static final String SERVICE_AUTHORIZATION_HEADER = "Bearer serviceToken";
    private static final String RENDITION_OUTPUT_URL = "http://dm-store/documents/123";

    @Autowired
    private DocAssemblyClient docAssemblyClient;

    @Pact(provider = "doc_assembly_template_rendition_provider", consumer = "pcs_api")
    public V4Pact generateDocument(PactDslWithProvider builder) {
        PactDslJsonBody requestBody = (PactDslJsonBody) new PactDslJsonBody()
            .stringType("templateId", TEMPLATE_ID)
            .stringType("outputType", "PDF")
            .object("formPayload")
            .stringType("ccdCaseReference", CCD_CASE_REFERENCE)
            .stringType("caseName", "Test Case")
            .closeObject();

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringType("renditionOutputLocation", RENDITION_OUTPUT_URL);

        return builder
            .given("a template can be rendered successfully")
            .uponReceiving("a request to create a template rendition")
            .method("POST")
            .path("/api/template-renditions")
            .headers(Map.of(
                "Authorization", AUTHORIZATION_HEADER,
                "ServiceAuthorization", SERVICE_AUTHORIZATION_HEADER,
                "Content-Type", "application/json"))
            .body(requestBody)
            .willRespondWith()
            .status(HttpStatus.CREATED.value())
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethods = "generateDocument")
    void shouldGenerateDocumentFromDocAssembly() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode formPayloadJson = mapper.readTree("{"
                                                       + "\"ccdCaseReference\":\"" + CCD_CASE_REFERENCE + "\","
                                                       + "\"caseName\":\"Test Case\""
                                                       + "}");

        // Create FormPayload and DocAssemblyRequest using proper builder pattern
        JsonNodeFormPayload formPayload = new JsonNodeFormPayload(formPayloadJson);

        DocAssemblyRequest request = DocAssemblyRequest.builder()
            .templateId(TEMPLATE_ID)
            .outputType(OutputType.PDF)
            .formPayload(formPayload)
            .outputFilename(null) // can be null for this test
            .secureDocStoreEnabled(false)
            .caseTypeId(null) // can be null for this test
            .jurisdictionId(null) // can be null for this test
            .build();

        DocAssemblyResponse response = docAssemblyClient.generateOrder(
            AUTHORIZATION_HEADER,
            SERVICE_AUTHORIZATION_HEADER,
            request
        );

        assertThat(response.getRenditionOutputLocation()).isNotNull();
        assertThat(response.getRenditionOutputLocation()).contains(RENDITION_OUTPUT_URL);
    }
}
