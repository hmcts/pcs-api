package uk.gov.hmcts.reform.pcs.dmstore;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.dmstore.wiremock.AddDocumentIdHeaderTransformer;
import uk.gov.hmcts.reform.pcs.dmstore.wiremock.SaveDocumentFileTransformer;
import uk.gov.hmcts.reform.pcs.dmstore.wiremock.ServeDocumentFileTransformer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;

@Component
@RequiredArgsConstructor
public class MockDmStoreServer {

    private final WireMockServer wireMockServer;

    @PostConstruct
    public void stubEndpoints() {
        stubGetDocumentEndpoints();
        stubCreateDocumentEndpoint();
        stubPatchDocumentEndpoint();
    }

    private void stubGetDocumentEndpoints() {
        String getDocumentMetadataResponse = """
            {
              "originalDocumentName": "Test document",
              "_links": {
                "self": {
                  "href": "{{request.baseUrl}}/documents/{{request.path.documentId}}"
                },
                "binary": {
                  "href": "{{request.baseUrl}}/documents/{{request.path.documentId}}/binary"
                }
              },
              "ttl": "2050-01-01T00:00:00.000Z",
              "metadata": {
                "case_type_id": "PCS",
                "jurisdiction": "PCS"
              }
            }
            """;

        wireMockServer
            .stubFor(WireMock.get(urlPathTemplate("/documents/{documentId}"))
                         .persistent(false)
                         .willReturn(WireMock.ok()
                                         .withHeader("Content-Type", "application/json")
                                         .withBody(getDocumentMetadataResponse)
                                         .withTransformers(ResponseTemplateTransformer.NAME)
                         ));


        wireMockServer
            .stubFor(WireMock.get(urlPathTemplate("/documents/{documentId}/binary"))
                         .persistent(false)
                         .willReturn(aResponse()
                                         .withTransformers(ServeDocumentFileTransformer.NAME)
                         ));

    }

    private void stubCreateDocumentEndpoint() {
        String responseJson = """
            {
              "_embedded": {
                "documents": [{
                  "originalDocumentName": "Test document",
                  "_links": {
                    "self": {
                      "href": "{{request.baseUrl}}/documents/{{request.headers.X-document-id}}"
                    },
                    "binary": {
                      "href": "{{request.baseUrl}}/documents/{{request.headers.X-document-id}}/binary"
                    }
                  },
                  "ttl": "2050-01-01T00:00:00.000Z",
                  "size": 123456,
                  "metadata": {
                    "case_type_id": "PCS",
                    "jurisdiction": "PCS"
                  }
                }]
              }
            }
            """;

        wireMockServer
            .stubFor(WireMock.post("/documents")
                         .persistent(false)
                         .willReturn(WireMock.ok()
                                         .withHeader("Content-Type", "application/json")
                                         .withBody(responseJson)
                                         .withTransformers(
                                             AddDocumentIdHeaderTransformer.NAME,
                                             SaveDocumentFileTransformer.NAME,
                                             ResponseTemplateTransformer.NAME
                                         )
                         ));
    }

    private void stubPatchDocumentEndpoint() {
        wireMockServer
            .stubFor(WireMock.patch("/documents")
                         .persistent(false)
                         .willReturn(WireMock.ok()
                                         .withHeader("Content-Type", "application/json")
                                         .withBody("{}")));
    }

}
