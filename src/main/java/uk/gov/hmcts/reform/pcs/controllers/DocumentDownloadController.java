package uk.gov.hmcts.reform.pcs.controllers;

import jakarta.servlet.http.HttpServletResponse;
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
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public void downloadDocumentById(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @PathVariable UUID documentId,
        HttpServletResponse response
    ) {

        DownloadedDocumentResponse document = documentDownloadService.downloadDocument(authorisation, documentId);

        response.setContentType(document.mimeType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(document.fileName()));

        try {
            try (InputStream in = document.file().getInputStream();
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }

            log.debug("Document streamed successfully: {}", document.fileName());

        } catch (IOException ioe) {
            log.error("Failed to stream document {}: {}", documentId, ioe.getMessage(), ioe);
            throw new DocumentDownloadException("Failed to stream document " + documentId, ioe);
        }

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
