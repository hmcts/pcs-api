package uk.gov.hmcts.reform.pcs.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * dm-store on the shared Wiremock (8083). Its default metadata has no case_id, so CDAM serves those documents
 * without asking CCD whether the caller can see them. A document attached here gets a case_id, so CDAM only
 * serves it if CCD returns it in the case data the caller can read, as in a deployed environment.
 */
@Component
public class DmStoreStub {

    private static final String DM_STORE_URL = "http://localhost:8083";

    private final WireMock dmStore = new WireMock("localhost", 8083);

    public StubMapping attachToCase(UUID documentId, long caseReference) {
        String documentUrl = DM_STORE_URL + "/documents/" + documentId;
        return dmStore.register(get(urlPathEqualTo("/documents/" + documentId))
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
        return dmStore.getServeEvents().stream()
            .filter(event -> event.getStubMapping() != null && stub.getId().equals(event.getStubMapping().getId()))
            .count();
    }

    public void remove(StubMapping stub) {
        dmStore.removeStubMapping(stub);
    }
}
