package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.pcs.ccd.ListValueUtils.unwrapListItems;

@Component
public class AddParties implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyService partyService;

    public AddParties(PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.addParties.name(), this::submit)
            .forAllStates()
            .name("Add parties")
            .showSummary()
            .grant(Permission.CR, UserRole.CREATOR_WITH_UPDATE, UserRole.UPDATER)
            .fields()
            .page("parties")
            .mandatory(
                PcsCase::getPartiesToAdd,
                "",
                emptyList(),
                "Parties",
                "Add more claimants, defendants and other interested parties here."
            )
            .done();
    }


    private void submit(EventPayload<PcsCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        PcsCase pcsCase = eventPayload.caseData();
        List<Party> partiesToAdd = unwrapListItems(pcsCase.getPartiesToAdd());

        partyService.addParties(caseReference, partiesToAdd);
    }

}
