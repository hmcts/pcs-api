package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Component
public class ClaimsListRenderer {

    private final PebbleEngine pebbleEngine;

    public ClaimsListRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(List<Claim> claimsList, long caseReference) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("claimsList");
        Writer writer = new StringWriter();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "claims", claimsList,
            "createClaimEvent", EventId.createClaim,
            "addClaimantEvent", EventId.addClaimantToClaim,
            "addDefendantEvent", EventId.addDefendantToClaim,
            "addInterestedPartyEvent", EventId.addInterestedPartyToClaim
        );

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

}
