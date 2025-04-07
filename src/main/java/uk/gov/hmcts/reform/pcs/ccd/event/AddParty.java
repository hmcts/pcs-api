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

@Component
public class AddParty implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyService partyService;

    public AddParty(PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.addParty.name(), this::submit)
            .forAllStates()
            .name("Add party")
            .showSummary()
            .grant(Permission.CR, UserRole.CREATOR_WITH_UPDATE, UserRole.UPDATER)
            .fields()
            .page("party")
            .mandatory(PcsCase::getCurrentParty)
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        PcsCase pcsCase = eventPayload.caseData();
        Party partyToAdd = pcsCase.getCurrentParty();

        partyService.addParties(caseReference, List.of(partyToAdd));
    }

}
