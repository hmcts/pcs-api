package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@AllArgsConstructor
@Component
public class RepaymentTableRenderer {

    private final PebbleEngine pebbleEngine;
    private final RepaymentTableHelper repaymentTableHelper;

    public String render(EnforcementCosts enforcementCosts, String template) {
        return render(enforcementCosts, null, template);
    }

    public String render(EnforcementCosts enforcementCosts, String caption, String template) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(template);
        Writer writer = new StringWriter();

        Map<String, Object> context = repaymentTableHelper.getContext(enforcementCosts, caption);

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }
}
