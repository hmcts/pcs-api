package uk.gov.hmcts.reform.pcs.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
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
            log.info("Calling document management API...");

            // Call the document API
            ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
                authorisation,
                serviceAuth,
                documentUuid
            );

            log.info("Response status: {}", response.getStatusCode());

            String mimeType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : "application/octet-stream";

            String fileName = response.getHeaders().getFirst("original-file-name");
            if (fileName == null || fileName.isEmpty()) {
                fileName = documentId;
            }

            log.info("Document found: {}, Type: {}", fileName, mimeType);

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
}
