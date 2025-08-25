package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType.COUNTER_CLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType.MAIN_CLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.updateCounterClaim;


@Component
public class ClaimListRenderer {

    private final PebbleEngine pebbleEngine;

    public ClaimListRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(long caseReference, List<Claim> claims, Map<UUID, List<CounterClaimEvent>> claimActionMap) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("claimList");

        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "mainClaims", filterByClaimType(claims, MAIN_CLAIM),
            "counterClaims", filterByClaimType(claims, COUNTER_CLAIM),
            "claimActionMap", claimActionMap,
            "updateCounterClaimEvent", updateCounterClaim.name()
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Failed to render template", ex);
        }

        return writer.toString();
    }

    private static List<Claim> filterByClaimType(List<Claim> claims, ClaimType claimType) {
        return claims.stream()
            .filter(claim -> claimType.equals(claim.getType()))
            .toList();
    }

}
