package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BulkClaimDetailsRenderer {

    private static final String TEMPLATE_PATH = "tabs/bulk-claim-details";

    private final PebbleEngine pebbleEngine;

    public String render(BulkClaimDetails details) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(TEMPLATE_PATH);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, Map.of("details", details));
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

}
