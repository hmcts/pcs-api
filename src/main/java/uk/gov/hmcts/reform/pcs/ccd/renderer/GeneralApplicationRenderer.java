package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;


@Component
public class GeneralApplicationRenderer {

    private final PebbleEngine pebbleEngine;

    public GeneralApplicationRenderer(
                                      PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(List<ListValue<GeneralApplication>> genAppList, Long caseReference) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("generalApplication");
        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(// things to add to context for template to pickup
                                             "caseReference", caseReference,
                                             "generalApplications", genAppList,
                                             "deleteDraftGeneralApplication", EventId.deleteDraftGeneralApplication
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render template", e);
        }
        return writer.toString();
    }

}
