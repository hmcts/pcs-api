package uk.gov.hmcts.reform.pcs.controllers;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;

@Slf4j
@RestController
@RequestMapping("/case/document")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final DocumentDownloadService documentDownloadService;

    @PostConstruct
    public void init() {
        log.info("=== DocumentDownloadController initialized ===");
        log.info("Endpoint: /case/document/downloadDocument/{documentId}");
    }

    @GetMapping("/downloadDocument/{documentId}")
    public void downloadDocumentById(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @PathVariable String documentId,
        HttpServletResponse response
    ) {
        try {
            DownloadedDocumentResponse document = documentDownloadService.downloadDocument(authorisation, documentId);

            log.info("Setting response headers for file: {}", document.fileName());

            // Set content type
            response.setContentType(document.mimeType());

            // Build Content-Disposition header with proper encoding
            String encodedFilename = java.net.URLEncoder.encode(document.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20"); // Replace + with %20 for better browser compatibility

            String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                sanitizeFilename(document.fileName()),
                encodedFilename
            );

            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

            log.info("Content-Disposition header set to: {}", contentDisposition);

            // STREAMING: CDAM InputStream → HTTP response
            try (InputStream in = document.file().getInputStream();
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }

            log.info("Document streamed successfully: {}", document.fileName());

        } catch (Exception e) {
            log.error("Failed to stream document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to stream document " + documentId, e);
        }
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
