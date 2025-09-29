package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.*;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "doc_assembly_template_rendition_provider", port = "8080")
public class DocAssemblyConsumerTest {

    private static final String TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";
    private static final String CCD_CASE_REFERENCE = "1234567891011121";
    private static final String AUTHORIZATION_HEADER = "Bearer userToken";
    private static final String SERVICE_AUTHORIZATION_HEADER = "Bearer serviceToken";
    private static final String RENDITION_OUTPUT_URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_RENDITION_API_PATH = "/api/template-renditions";

    @Pact(provider = "doc_assembly_template_rendition_provider", consumer = "pcs_api")
    public V4Pact generateDocument(PactDslWithProvider builder) {
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
            .body(createRequestDsl())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(responseBody)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethods = "generateDocument")
    void shouldGenerateDocumentFromDocAssembly(MockServer mockServer) {
        Response response = given()
            .header("Authorization", AUTHORIZATION_HEADER)
            .header("ServiceAuthorization", SERVICE_AUTHORIZATION_HEADER)
            .contentType(ContentType.JSON)
            .body(createRequestDsl().getBody().toString())
            .post(mockServer.getUrl() + TEMPLATE_RENDITION_API_PATH)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        String renditionLocation = response.jsonPath().getString("renditionOutputLocation");

        assertThat(renditionLocation).isNotNull();
        assertThat(renditionLocation).contains(RENDITION_OUTPUT_URL);
    }

    private DslPart createRequestDsl() {
        return newJsonBody(this::buildRequestBody).build();
    }

    private void buildRequestBody(LambdaDslObject body) {
        body.object("formPayload", formPayload -> {
            formPayload.stringType("caseName", "Test Case")
                .stringType("ccdCaseReference", CCD_CASE_REFERENCE);
        });
        body.stringType("outputType", "PDF")
            .stringType("templateId", TEMPLATE_ID);
    }
}
