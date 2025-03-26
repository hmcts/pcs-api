package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Component
public class DeactivateParties implements CCDConfig<PCSCase, State, UserRole> {

    private final PartyRepository partyRepository;

    public DeactivateParties(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.deactivateParties.name(), this::submit)
            .forAllStates()
            .name("Deactivate parties")
            .showCondition("activePartiesEmpty=\"No\"")
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("deactivate-parties")
            .mandatory(
                PCSCase::getPartiesToDeactivate,
                "",
                emptyList(),
                "Parties to deactivate",
                "Tick the parties you wish to deactivate"
            )
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase pcsCase = caseDetails.getData();

        List<DynamicListElement> optionsList = partyRepository.findAllDtoByCaseReference(caseDetails.getId(), true)
            .stream()
            .map(party -> {
                String partyName = party.getForename() + " " + party.getSurname();
                return DynamicListElement.builder().code(party.getId()).label(partyName).build();
            })
            .toList();

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(optionsList)
            .build();
        pcsCase.setPartiesToDeactivate(dynamicList);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.payload();

        // TODO: Check permissions here?
        DynamicMultiSelectList partiesToDeactivate = pcsCase.getPartiesToDeactivate();

        List<UUID> uuidsToDeactivate = partiesToDeactivate.getValue().stream()
            .map(DynamicListElement::getCode)
            .toList();

        partyRepository.setPartiesActive(uuidsToDeactivate, false);
    }


}
