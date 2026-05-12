package uk.gov.hmcts.reform.pcs.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/case/document")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final DocumentDownloadService documentDownloadService;

    @GetMapping("/downloadDocument/{documentId}")
    public ResponseEntity<StreamingResponseBody> downloadDocumentById(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @PathVariable UUID documentId
    ) {
        DownloadedDocumentResponse document = documentDownloadService.downloadDocument(authorisation, documentId);

        StreamingResponseBody streamBody = out -> {
            try (InputStream in = document.file().getInputStream()) {
                in.transferTo(out);
            }
        };

        return ResponseEntity.status(HttpStatus.OK)
            .contentType(document.mediaType())
            .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(document.fileName()))
            .body(streamBody);
    }

    private String buildContentDisposition(String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
            .replace("+", "%20"); // Replace + with %20 for better browser compatibility

        return String.format(
            "attachment; filename=\"%s\"; filename*=UTF-8''%s",
            sanitizeFilename(filename),
            encodedFilename
        );
    }

    /**
     * Sanitize filename for use in Content-Disposition header.
     * Removes or replaces characters that might cause issues.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "document";
        }
        // Remove or replace problematic characters
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
