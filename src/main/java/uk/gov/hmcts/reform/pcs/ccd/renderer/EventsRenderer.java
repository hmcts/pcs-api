package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Component
public class EventsRenderer {

    private final PebbleEngine pebbleEngine;

    public EventsRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(long caseReference, PcsCase pcsCase, List<String> roles) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("events");
        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "pcsCase", pcsCase,
            "roles", roles
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

    public boolean hasRole() {
        return true;
    }

}
