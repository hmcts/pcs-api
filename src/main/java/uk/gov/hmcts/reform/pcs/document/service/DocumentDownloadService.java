package uk.gov.hmcts.reform.pcs.document.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDownloadService {

    private static final MediaType DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM;
    private static final String FALLBACK_FILENAME = "Unknown filename";

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentRepository documentRepository;

    public DownloadedDocumentResponse downloadDocument(String authorisation, UUID documentId) {
        try {
            String serviceAuth = authTokenGenerator.generate();

            ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
                authorisation,
                serviceAuth,
                documentId
            );

            String fileName = documentRepository.findByDocumentId(documentId)
                .map(DocumentEntity::getFileName)
                .orElseGet(() -> getOriginalFileName(authorisation, serviceAuth, documentId));

            MediaType mediaType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType()
                : DEFAULT_CONTENT_TYPE;

            log.debug("Document downloaded successfully: {}, Type: {}", fileName, mediaType);

            return new DownloadedDocumentResponse(
                response.getBody(),
                fileName,
                mediaType
            );

        } catch (FeignException fe) {
            log.error("Failed to download document: {}", documentId, fe);
            throw new DocumentDownloadException("Failed to stream document " + documentId, fe);
        }
    }

    private String getOriginalFileName(String authorisation, String serviceAuth, UUID documentId) {
        try {
            // Fetch document metadata from CDAM
            log.info("Fetching document metadata from CDAM...");
            Document documentMetadata = caseDocumentClientApi.getMetadataForDocument(
                authorisation,
                serviceAuth,
                documentId
            );

            if (documentMetadata != null && documentMetadata.originalDocumentName != null
                && !documentMetadata.originalDocumentName.isEmpty()) {
                log.debug("Original filename retrieved from metadata: {}", documentMetadata.originalDocumentName);
                return documentMetadata.originalDocumentName;
            }

            log.warn("Document metadata does not contain original filename, using default as fallback");
            return FALLBACK_FILENAME;

        } catch (FeignException fe) {
            log.warn("Failed to fetch document metadata, using default name as fallback. Error: {}", fe.getMessage());
            return FALLBACK_FILENAME;
        }
    }
}
