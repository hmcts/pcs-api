package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;


@Component
public class ClaimHistoryRenderer {

    private final PebbleEngine pebbleEngine;

    public ClaimHistoryRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    // TODO: RBAC for claims history
    public String render(long caseReference, List<Claim> claims) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("claimHistory");

        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "counterClaims", filterToCounterClaims(claims)
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

    private static List<Claim> filterToCounterClaims(List<Claim> claims) {
        return claims.stream()
            .filter(claim -> ClaimType.COUNTER_CLAIM.equals(claim.getType()))
            .toList();
    }

}
