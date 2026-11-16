package uk.gov.hmcts.reform.pcs.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Attaches dm-store documents to a case so CDAM checks access through CCD before serving them.
 */
@Component
@RequiredArgsConstructor
public class DocumentServiceStubs {

    private final WireMockServer wireMockServer;

    public StubMapping attachToCase(UUID documentId, long caseReference) {
        String documentUrl = wireMockServer.baseUrl() + "/documents/" + documentId;
        return wireMockServer.stubFor(get(urlPathEqualTo("/documents/" + documentId))
            .atPriority(1)
            .willReturn(okJson("""
                {
                  "originalDocumentName": "Test document",
                  "mimeType": "application/pdf",
                  "_links": {
                    "self": { "href": "%s" },
                    "binary": { "href": "%s/binary" }
                  },
                  "metadata": {
                    "case_id": "%d",
                    "case_type_id": "PCS",
                    "jurisdiction": "PCS"
                  }
                }
                """.formatted(documentUrl, documentUrl, caseReference))));
    }

    public long requestsServedBy(StubMapping stub) {
        return wireMockServer.getAllServeEvents().stream()
            .filter(event -> event.getStubMapping() != null && stub.getId().equals(event.getStubMapping().getId()))
            .count();
    }

    public void remove(StubMapping stub) {
        wireMockServer.removeStub(stub);
    }
}
