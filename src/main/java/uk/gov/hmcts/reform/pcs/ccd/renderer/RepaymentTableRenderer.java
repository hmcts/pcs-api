package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Component
public class RepaymentTableRenderer {
    private final PebbleEngine pebbleEngine;

    public String render(BigDecimal totalArrears, BigDecimal legalFees, BigDecimal landRegistryFees,
                         String warrantFeeAmount, BigDecimal totalFees) {

        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("repaymentTable");
        Writer writer = new StringWriter();

        Map<String, Object> context = new HashMap<>();
        context.put("totalArrears", totalArrears);
        context.put("legalFees", legalFees);
        context.put("landRegistryFees", landRegistryFees);
        context.put("warrantFeeAmount", warrantFeeAmount);
        context.put("totalFees", totalFees);

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }
}
