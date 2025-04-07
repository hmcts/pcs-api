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
public class ReactivateParties implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyRepository partyRepository;

    public ReactivateParties(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.reactivateParties.name(), this::submit)
            .forAllStates()
            .name("Reactivate parties")
            .showCondition("inactivePartiesEmpty=\"No\"")
            .aboutToStartCallback(this::start)
            .grant(Permission.CR, UserRole.CREATOR_WITH_UPDATE, UserRole.UPDATER)
            .fields()
            .page("reactivate-parties")
            .mandatory(
                PcsCase::getPartiesToReactivate,
                "",
                emptyList(),
                "Parties to reactivate",
                "Tick the parties you wish to reactivate"
            )
            .done();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> start(CaseDetails<PcsCase, State> caseDetails) {
        PcsCase pcsCase = caseDetails.getData();

        List<DynamicListElement> optionsList = partyRepository.findAllDtoByCaseReference(caseDetails.getId(), false)
            .stream()
            .map(party -> {
                String partyName = party.getForename() + " " + party.getSurname();
                return DynamicListElement.builder().code(party.getId()).label(partyName).build();
            })
            .toList();

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(optionsList)
            .build();

        pcsCase.setPartiesToReactivate(dynamicList);

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }


    private void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();

        DynamicMultiSelectList partiesToReactivate = pcsCase.getPartiesToReactivate();

        List<UUID> uuidsToReactivate = partiesToReactivate.getValue().stream()
            .map(DynamicListElement::getCode)
            .toList();

        partyRepository.setPartiesActive(uuidsToReactivate, true);
    }


}
