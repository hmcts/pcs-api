package uk.gov.hmcts.reform.pcs.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Doc assembly and dm-store on the shared Wiremock (8083). By default every rendition gets the same document id,
 * and dm-store's metadata has no case_id, so CDAM serves documents without asking CCD whether the caller can
 * see them. These stubs give each rendition its own id and attach documents to a case, so CDAM only serves one
 * if CCD returns it in the case data the caller can read, as in a deployed environment.
 */
@Component
public class DocumentServiceStubs {

    private static final String DM_STORE_URL = "http://localhost:8083";

    private final WireMock wireMock = new WireMock("localhost", 8083);

    public StubMapping giveEachRenditionItsOwnId() {
        return wireMock.register(post(urlPathEqualTo("/api/template-renditions"))
            .atPriority(1)
            .willReturn(okJson("""
                { "renditionOutputLocation": "%s/documents/{{randomValue type='UUID'}}" }
                """.formatted(DM_STORE_URL))));
    }

    public StubMapping attachToCase(UUID documentId, long caseReference) {
        String documentUrl = DM_STORE_URL + "/documents/" + documentId;
        return wireMock.register(get(urlPathEqualTo("/documents/" + documentId))
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
        return wireMock.getServeEvents().stream()
            .filter(event -> event.getStubMapping() != null && stub.getId().equals(event.getStubMapping().getId()))
            .count();
    }

    public void remove(StubMapping stub) {
        wireMock.removeStubMapping(stub);
    }
}
