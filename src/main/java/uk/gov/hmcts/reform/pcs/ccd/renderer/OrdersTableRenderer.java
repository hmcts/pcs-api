package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;
import static uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload.ExistingApplicationDocumentLinkBuilder.documentLink;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
        context.put("documentName",
                    documentEntity == null ? null : documentEntity.getFileName());

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render orders table", e);
        }

        return writer.toString();
    }
}
