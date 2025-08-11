package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Defendant;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Component
public class DefendantsTabRenderer {

    private final PebbleEngine pebbleEngine;


    public DefendantsTabRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(List<ListValue<Defendant>> defendants) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("defendantsTab");

        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "defendants", defendants
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

}
