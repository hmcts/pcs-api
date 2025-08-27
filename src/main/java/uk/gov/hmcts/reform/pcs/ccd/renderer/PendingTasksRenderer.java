package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenApp;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType.COUNTER_CLAIM;


@Component
public class PendingTasksRenderer {

    private final PebbleEngine pebbleEngine;

    public PendingTasksRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(long caseReference, List<GenApp> genApps, Map<UUID, List<GenAppEvent>> genAppActionMap) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("pendingTasks");

        Writer writer = new StringWriter();

        boolean pendingTasksAvailable = genApps.stream()
            .anyMatch(genApp -> genApp.getState() == GenAppState.ISSUED);

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "genAppActionMap", genAppActionMap,
            "genApps", genApps,
            "pendingTasksAvailable", pendingTasksAvailable
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

}
