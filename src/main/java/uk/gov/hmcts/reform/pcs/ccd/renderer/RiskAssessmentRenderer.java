package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class RiskAssessmentRenderer {

    private final PebbleEngine pebbleEngine;

    public String render() {

        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("riskAssessment");
        Writer writer = new StringWriter();

        Map<String, Object> context = new HashMap<>();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }
}
