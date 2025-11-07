package uk.gov.hmcts.reform.pcs.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;

import jakarta.annotation.PostConstruct;

@Slf4j
@RestController
@RequestMapping("/cases/{caseId}/documents")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final DocumentDownloadService documentDownloadService;

    @PostConstruct
    public void init() {
        log.info("=== DocumentDownloadController initialized ===");
        log.info("Endpoint available at: /cases/{caseId}/documents/{documentId}/download");
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        log.info("=== Test endpoint called ===");
        return ResponseEntity.ok("DocumentDownloadController is working!");
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocumentById(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @PathVariable String caseId,
        @PathVariable String documentId
    ) {
        log.info("=== DocumentDownloadController.downloadDocumentById called ===");
        log.info("Download request for caseId: {}, documentId: {}", caseId, documentId);
        log.info("Authorization header present: {}", authorisation != null);

        DownloadedDocumentResponse documentResponse = documentDownloadService.downloadDocument(
            authorisation,
            documentId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(documentResponse.mimeType()));
        headers.set("original-file-name", documentResponse.fileName());

        log.info("Document download successful: {}", documentId);
        return ResponseEntity.ok()
            .headers(headers)
            .body(documentResponse.file());
    }
}
