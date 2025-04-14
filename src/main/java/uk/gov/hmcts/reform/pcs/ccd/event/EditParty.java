package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

@Component
public class EditParty implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository cases;

    @Autowired
    private PartyRepository parties;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("editParty", this::submit, this::start)
            .forState(State.Open)
            .name("Edit a party")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .showCondition("[STATE]=\"NEVER_SHOW\"") // Hide from the event drop down, we link to it.
            .fields()
            .page("Party details")
                .mandatory(PCSCase::getPartyFirstName)
                .mandatory(PCSCase::getPartyLastName)
            .done();
    }

    // Prepopulate the form with the party's details
    private PCSCase start(EventPayload<PCSCase, State> payload) {
        var c = cases.getReferenceById(payload.caseReference());
        var result = payload.caseData();
        var party = c.findPartyOrThrow(Long.valueOf(payload.urlParams().getFirst("partyId")));
        result.setPartyFirstName(party.getForename());
        result.setPartyLastName(party.getSurname());
        return result;
    }

    // Prepopulate the form with the party's details
    // TODO: Concurrency control.
    //  Use a party version column + Hibernate's optimistic lock to reject concurrent updates to the same party.
    private void submit(EventPayload<PCSCase, State> payload) {
        var c = cases.getReferenceById(payload.caseReference());
        var party = c.findPartyOrThrow(Long.valueOf(payload.urlParams().getFirst("partyId")));
        var data = payload.caseData();
        party.setForename(data.getPartyFirstName());
        party.setSurname(data.getPartyLastName());
        parties.save(party);
    }
}
