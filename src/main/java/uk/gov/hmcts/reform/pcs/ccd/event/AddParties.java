package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.pcs.ccd.ListValueUtils.unwrapListItems;

@Component
public class AddParties implements CCDConfig<PCSCase, State, UserRole> {

    private final PartyService partyService;
    private final PCSCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;

    public AddParties(PartyService partyService,
                      PCSCaseRepository pcsCaseRepository,
                      PartyRepository partyRepository) {
        this.partyService = partyService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.addParties.name(), this::submit)
            .forAllStates()
            .name("Add parties")
            .showSummary()
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("parties")
            .mandatory(
                PCSCase::getPartiesToAdd,
                "",
                emptyList(),
                "Parties",
                "Add more claimants, defendants and other interested parties here."
            )
            .done();
    }


    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        PCSCase pcsCase = eventPayload.payload();
        List<Party> partiesToAdd = unwrapListItems(pcsCase.getPartiesToAdd());

        partyService.addParties(caseReference, partiesToAdd);
    }

}
