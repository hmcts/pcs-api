package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessagesRenderer {

    private static final String TEMPLATE_PATH = "tabs/messages";

    private final PebbleEngine pebbleEngine;

    /** A message thread as the tab shows it; decideUrl is set for an open message about an open application. */
    public record Thread(String subject, String categories, String application, String decideUrl, String from,
                         String to, boolean urgent, LocalDateTime sent, LocalDateTime updated,
                         List<Entry> entries, String closedBy, String closureNote) {
    }

    public record Entry(String from, String to, boolean urgent, LocalDateTime sent, String body) {
    }

    public String render(List<Thread> open, List<Thread> closed) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(TEMPLATE_PATH);
        Writer writer = new StringWriter();
        try {
            compiledTemplate.evaluate(writer, Map.of("open", open, "closed", closed));
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }
        return writer.toString();
    }
}
