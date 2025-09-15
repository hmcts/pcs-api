package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;


@Component
public class SummaryForDefendantRenderer {

    private final PebbleEngine pebbleEngine;


    public SummaryForDefendantRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(PCSCase pcsCase) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("summaryForDefendant");

        Writer writer = new StringWriter();

        Map<String, Object> context = new HashMap<>();
        context.put("propertyAddress", pcsCase.getPropertyAddress());
        if (pcsCase.getDefendantResponse() != null) {
            context.put("defendantResponse", pcsCase.getDefendantResponse());
        }

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

}
