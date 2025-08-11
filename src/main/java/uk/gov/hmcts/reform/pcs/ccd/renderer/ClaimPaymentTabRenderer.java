package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;


@Component
public class ClaimPaymentTabRenderer {

    private final PebbleEngine pebbleEngine;


    public ClaimPaymentTabRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(Long caseRef, PaymentStatus paymentStatus) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("claimPaymentTab");

        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseRef,
            "paymentStatus", paymentStatus.getLabel(),
            "processClaimPayment", EventId.processClaimPayment.name()
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

}
