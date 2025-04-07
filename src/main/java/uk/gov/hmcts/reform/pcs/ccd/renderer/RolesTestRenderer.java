package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RolesTestRenderer {

    private final PebbleEngine pebbleEngine;

    public RolesTestRenderer(PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
    }

    public String render(List<Party> allParties, List<Claim> claimsList, long caseReference) {
        return "## Roles test";
//        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("rolesTest");


//        Map<Party, Map<PartyRole, List<Claim>>> partyClaimsMap = new LinkedHashMap<>();
//
//        allParties.forEach(party -> partyClaimsMap.put(party, new LinkedHashMap<>()));
//
//        for (Claim claim : claimsList) {
//            populatePartyClaimsMap(partyClaimsMap, claim.getClaimants(), PartyRole.CLAIMANT, claim);
//            populatePartyClaimsMap(partyClaimsMap, claim.getDefendants(), PartyRole.DEFENDANT, claim);
//            populatePartyClaimsMap(partyClaimsMap, claim.getInterestedParties(), PartyRole.INTERESTED_PARTY, claim);
//        }
//
//        Writer writer = new StringWriter();
//
//        Map<String, Object> context = Map.of(
//            "caseReference", caseReference,
//            "partyClaimsMap", partyClaimsMap,
//            "editPartyEvent", EventId.editParty,
//            "addPartyEvent", EventId.addParty
//        );
//
//        try {
//            compiledTemplate.evaluate(writer, context);
//        } catch (IOException e) {
//            throw new TemplateRenderingException("Failed to render template", e);
//        }
//
//        return writer.toString();
    }

    private static void populatePartyClaimsMap(Map<Party, Map<PartyRole, List<Claim>>> partyClaimsMap,
                                               List<Party> partyList,
                                               PartyRole partyRole,
                                               Claim claim) {

        partyList.forEach(
            party -> {
                Map<PartyRole, List<Claim>> partyClaims = partyClaimsMap.computeIfAbsent(
                    party,
                    c -> new LinkedHashMap<>()
                );
                List<Claim> claimsListForRole = partyClaims.computeIfAbsent(partyRole, r -> new ArrayList<>());
                claimsListForRole.add(claim);
            }
        );
    }

}
