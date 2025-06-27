package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Component
public class NextStepsRenderer {

    private final PebbleEngine pebbleEngine;

    public NextStepsRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(long caseReference, boolean draftExists) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("nextSteps");

        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "draftExists", draftExists,
            "continueEvent", EventId.continueCaseCreation
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

}
