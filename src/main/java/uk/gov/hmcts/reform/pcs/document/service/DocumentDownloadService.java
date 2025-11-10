package uk.gov.hmcts.reform.pcs.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDownloadService {

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;

    public DownloadedDocumentResponse downloadDocument(String authorisation, String documentId) {
        log.info("Downloading document: {}", documentId);

        try {
            // Generate S2S token
            String serviceAuth = authTokenGenerator.generate();

            // Parse UUID
            UUID documentUuid = UUID.fromString(documentId);

            // Call the document API
            ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
                authorisation,
                serviceAuth,
                documentUuid
            );

            // Extract metadata from response headers
            String mimeType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : "application/octet-stream";

            String fileName = response.getHeaders().getFirst("original-file-name");
            if (fileName == null || fileName.isEmpty()) {
                fileName = documentId;
            }

            log.info("Document downloaded successfully: {}, Type: {}", fileName, mimeType);

            return new DownloadedDocumentResponse(
                response.getBody(),
                fileName,
                mimeType
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid document ID format: {}", documentId, e);
            throw new DocumentDownloadException(documentId, e);
        } catch (Exception e) {
            log.error("Failed to download document: {}", documentId, e);
            throw new DocumentDownloadException(documentId, e);
        }
    }
}
