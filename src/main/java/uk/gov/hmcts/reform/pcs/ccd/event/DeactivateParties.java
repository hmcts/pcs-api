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
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Component
public class DeactivateParties implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyRepository partyRepository;

    public DeactivateParties(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.deactivateParties.name(), this::submit)
            .forAllStates()
            .name("Deactivate parties")
            .showCondition("activePartiesEmpty=\"No\"")
            .aboutToStartCallback(this::start)
            .grant(Permission.CR, UserRole.CREATOR_WITH_UPDATE, UserRole.UPDATER)
            .fields()
            .page("deactivate-parties")
            .mandatory(
                PcsCase::getPartiesToDeactivate,
                "",
                emptyList(),
                "Parties to deactivate",
                "Tick the parties you wish to deactivate"
            )
            .done();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> start(CaseDetails<PcsCase, State> caseDetails) {
        PcsCase pcsCase = caseDetails.getData();

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

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();

        DynamicMultiSelectList partiesToDeactivate = pcsCase.getPartiesToDeactivate();

        List<UUID> uuidsToDeactivate = partiesToDeactivate.getValue().stream()
            .map(DynamicListElement::getCode)
            .toList();

        partyRepository.setPartiesActive(uuidsToDeactivate, false);
    }


}
