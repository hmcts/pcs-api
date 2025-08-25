package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenApp;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class GenAppListRenderer {

    private final PebbleEngine pebbleEngine;

    public GenAppListRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(long caseReference, List<GenApp> genApps, Map<UUID, List<GenAppEvent>> genAppActionMap) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("genAppList");

        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "genApps", genApps,
            "genAppActionMap", genAppActionMap
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

}
