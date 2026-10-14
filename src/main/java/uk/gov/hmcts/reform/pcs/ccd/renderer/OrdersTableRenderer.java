package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrdersTableRenderer {
    private static final DateTimeFormatter UPLOADED_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.UK);

    private final PebbleEngine pebbleEngine;

    public String render(DocumentEntity documentEntity, LocalDateTime uploadedDate) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("ordersTable");
        Writer writer = new StringWriter();

        Map<String, Object> context = new HashMap<>();
        context.put(
            "uploadedOn", uploadedDate == null
                ? "Not available"
                : UPLOADED_DATE_FORMAT.format(uploadedDate)
        );
        context.put("documentUrl", documentLink(documentEntity));
        context.put("documentName", documentEntity.getFileName());

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render orders table", e);
        }

        return writer.toString();
    }

    private String documentLink(DocumentEntity documentEntity) {
        if (documentEntity == null) {
            return null;
        }
        UUID documentId = documentEntity.getDocumentId();
        if (documentId == null) {
            documentId = extractDocumentId(documentEntity.getUrl());
        }
        if (documentId == null) {
            documentId = extractDocumentId(documentEntity.getBinaryUrl());
        }
        if (documentId == null) {
            return null;
        }
        return "/documents/%s/binary".formatted(documentId);
    }

    private UUID extractDocumentId(String documentUrl) {
        if (documentUrl == null) {
            return null;
        }

        int documentPathStart = documentUrl.indexOf("/documents/");
        if (documentPathStart < 0) {
            return null;
        }

        String documentPath = documentUrl.substring(documentPathStart + "/documents/".length());
        String documentId = documentPath.split("/", 2)[0];
        try {
            return UUID.fromString(documentId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
