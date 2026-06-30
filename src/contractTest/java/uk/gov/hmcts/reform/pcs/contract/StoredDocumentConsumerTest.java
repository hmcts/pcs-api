package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static io.restassured.RestAssured.given;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "dm_store_stored_document_provider", port = "8080")

public class StoredDocumentConsumerTest {

    private static final String SERVICE_AUTHORIZATION = "Bearer serviceToken";
    private static final String DOCUMENT_ID = "969983aa-52ae-41bd-8cf3-4aabcc120783";
    private static final String USER_ID = "9a2d861a-6264-4765-9f61-1d403079f71b";
    private static final String APPLICATION = "application/vnd.uk.gov.hmcts.dm.document.v1+hal+json;charset=UTF-8";

    @Pact(provider = "dm_store_stored_document_provider", consumer = "pcs_api")
    public V4Pact getStoredDocumentPact(PactDslWithProvider builder) {
        return builder
            .given("A Stored Document exists and can be retrieved by documentId")
            .uponReceiving("GET request for a stored document by id")
            .path("/documents/" + DOCUMENT_ID)
            .method("GET")
            .headers(Map.of("ServiceAuthorization", SERVICE_AUTHORIZATION,"Accept", APPLICATION))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", APPLICATION))
            .body(buildResponseDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getStoredDocumentPact")
    void shouldFetchStoredDocument() {

        given()
            .accept(APPLICATION)
            .header("ServiceAuthorization", SERVICE_AUTHORIZATION)
            .when()
            .get("/documents/" + DOCUMENT_ID)
            .then()
            .statusCode(200);
    }

    private DslPart buildResponseDsl() {
        return newJsonBody(body ->
                               body.stringType("classification", "PUBLIC")
                                   .stringType("createdBy", USER_ID)
                                   .stringType("createdOn", "2024-01-01T12:00:00Z")
                                   .object(
                                       "_links", links ->
                                           links.object(
                                               "self", self ->
                                                   self.stringType("href", "http://localhost/documents/" + DOCUMENT_ID)
                                           )
                                   )
        ).build();
    }
}
