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
        log.info("Downloading document: {}", documentId);

        String serviceAuth = authTokenGenerator.generate();

        ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
            authorisation,
            serviceAuth,
            UUID.fromString(documentId)
        );

        log.info("Document downloaded successfully: {}", documentId);

        return new DownloadedDocumentResponse(
            response.getBody(),
            extractFilename(response),
            extractMimeType(response)
        );
    }

    private String extractFilename(ResponseEntity<Resource> response) {
        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            return contentDisposition.substring(contentDisposition.indexOf("filename=") + 9)
                .replaceAll("\"", "");
        }
        return "document.pdf";
    }

    private String extractMimeType(ResponseEntity<Resource> response) {
        if (response.getHeaders().getContentType() != null) {
            return response.getHeaders().getContentType().toString();
        }
        return "application/pdf";
    }
}
