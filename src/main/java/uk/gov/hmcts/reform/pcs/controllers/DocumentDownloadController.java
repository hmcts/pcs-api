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

import jakarta.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/case/document")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final DocumentDownloadService documentDownloadService;

    @GetMapping(value = "/downloadDocument/{documentId}")
    public ResponseEntity<Resource> downloadDocumentById(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @NotNull @PathVariable String documentId
    ) {
        System.out.println("****** CONTROLLER HIT ******");
        System.out.println("Document ID: " + documentId);
        log.error("******* CONTROLLER HIT - Document ID: {}", documentId);

        log.info("Download request received for document: {}", documentId);

        DownloadedDocumentResponse documentResponse = documentDownloadService.downloadDocument(
            authorisation,
            documentId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(documentResponse.mimeType()));
        headers.set("original-file-name", documentResponse.fileName());

        return ResponseEntity.ok()
            .headers(headers)
            .body(documentResponse.file());
    }
}
