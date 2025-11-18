package uk.gov.hmcts.reform.pcs.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDownloadService {

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;

    public DownloadedDocumentResponse downloadDocument(String authorisation, String documentId) {
        log.info("--- Downloading Document ---");
        log.info("Requested Document ID: {}", documentId);

        try {
            // Generate S2S token
            String serviceAuth = authTokenGenerator.generate();
            log.info("Generated S2S token for downstream call");

            // Parse UUID
            UUID documentUuid = UUID.fromString(documentId);

            // First, get document metadata to retrieve the original filename
            String fileName = getOriginalFileName(authorisation, serviceAuth, documentUuid, documentId);

            log.info("Calling document management API to download binary...");

            // Call the document API for binary content
            ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
                authorisation,
                serviceAuth,
                documentUuid
            );

            log.info("Response status: {}", response.getStatusCode());

            String mimeType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : "application/octet-stream";

            log.info("Document downloaded successfully: {}, Type: {}", fileName, mimeType);

            return new DownloadedDocumentResponse(
                response.getBody(),
                fileName,
                mimeType
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid document ID format: {}", documentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to download document: {}", documentId, e);
            throw e;
        }
    }

    private String getOriginalFileName(String authorisation, String serviceAuth, UUID documentUuid, String documentId) {
        try {
            // Fetch document metadata from CDAM
            log.info("Fetching document metadata from CDAM...");
            Document documentMetadata = caseDocumentClientApi.getMetadataForDocument(
                authorisation,
                serviceAuth,
                documentUuid
            );

            if (documentMetadata != null && documentMetadata.originalDocumentName != null
                && !documentMetadata.originalDocumentName.isEmpty()) {
                log.info("Original filename retrieved from metadata: {}", documentMetadata.originalDocumentName);
                return documentMetadata.originalDocumentName;
            }

            log.warn("Document metadata does not contain original filename, using UUID as fallback");
            return documentId;

        } catch (Exception e) {
            log.warn("Failed to fetch document metadata, will use UUID as fallback. Error: {}", e.getMessage());
            return documentId;
        }
    }
}
