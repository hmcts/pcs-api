package uk.gov.hmcts.reform.pcs.ccd.renderer;

import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.HashMap;

@Component
public class UserTypeInfoRenderer {

    private final PebbleEngine pebbleEngine;

    public UserTypeInfoRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(String userType, long caseReference) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("userTypeInfo");

        Map<String, Object> context = new HashMap<>();
        context.put("userType", userType);
        context.put("caseReference", caseReference);

        try (Writer writer = new StringWriter()) {
            compiledTemplate.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            System.out.println("Failed to render template");
            return "<p>Error rendering template</p>";
        }

        // return "<p>User Type: " + userType + "</p><p>Case Reference: " +
        // caseReference + "</p>";
    }
}