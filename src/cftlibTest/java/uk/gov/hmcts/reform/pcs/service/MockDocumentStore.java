package uk.gov.hmcts.reform.pcs.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serves real document content from the shared document-store Wiremock (8083). Its standing stub returns the same
 * placeholder PDF for every document; a stub added here takes priority for one document id, so a test can put files
 * of any type behind CDAM.
 */
@Service
public class MockDocumentStore {

    public static final String DOCUMENT_STORE_URL = "http://localhost:8083";

    private final RestClient admin = RestClient.create(DOCUMENT_STORE_URL + "/__admin/mappings");
    private final List<String> stubIds = new ArrayList<>();

    /**
     * Stores {@code content} under a new document id and returns the id.
     */
    public UUID store(byte[] content, String mimeType) {
        UUID documentId = UUID.randomUUID();
        Map<?, ?> created = admin.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "priority", 1,
                "request", Map.of("method", "GET", "urlPath", "/documents/" + documentId + "/binary"),
                "response", Map.of(
                    "status", 200,
                    "headers", Map.of("Content-Type", mimeType),
                    "base64Body", Base64.getEncoder().encodeToString(content))))
            .retrieve()
            .body(Map.class);
        stubIds.add(String.valueOf(created.get("id")));
        return documentId;
    }

    public String url(UUID documentId) {
        return DOCUMENT_STORE_URL + "/documents/" + documentId;
    }

    /**
     * Removes the stubs this test added, leaving the shared Wiremock as it was.
     */
    public void removeStoredDocuments() {
        stubIds.forEach(id -> admin.delete().uri("/{id}", id).retrieve().toBodilessEntity());
        stubIds.clear();
    }
}
