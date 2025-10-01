package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.List;

@Component
public class AddParty implements CCDConfig<PCSCase, State, UserRole> {

    private final PartyService partyService;

    public AddParty(PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.addParty.name(), this::submit)
            .forAllStates()
            .name("Add party")
            .showSummary()
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .fields()
            .page("party")
            .mandatory(PCSCase::getCurrentParty)
            .done();
    }


    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        PCSCase pcsCase = eventPayload.caseData();
        Party partyToAdd = pcsCase.getCurrentParty();

        partyService.addParties(caseReference, List.of(partyToAdd));
    }

}
