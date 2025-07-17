package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import io.pebbletemplates.pebble.PebbleEngine;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class TrialTemplateRenderer {
    private final PebbleEngine pebbleEngine;

    public TrialTemplateRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render() {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("trialTemplate");
        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "testString", "Toby"
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            System.out.println("Failed to render tempalte // I know I'm not using a logger");
        }

        return writer.toString();
    }
}
