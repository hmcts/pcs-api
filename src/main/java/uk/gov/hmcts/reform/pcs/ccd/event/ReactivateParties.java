package uk.gov.hmcts.reform.pcs.ccd.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ReactivateParties implements CCDConfig<PCSCase, State, UserRole> {

    private static final Logger logger = LoggerFactory.getLogger(ReactivateParties.class);

    private final PartyRepository partyRepository;

    public ReactivateParties(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.reactivateParties.name(), this::submit)
            .forAllStates()
            .name("Reactivate parties")
            .showCondition("inactivePartiesEmpty=\"No\"")
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("reactivate-parties")
            .mandatory(
                PCSCase::getPartiesToReactivate,
                "",
                emptyList(),
                "Parties to reactivate",
                "Tick the parties you wish to reactivate"
            )
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase pcsCase = caseDetails.getData();

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

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }


    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.payload();
        logger.info("About to submit {}", pcsCase);

        // TODO: Check permissions here?
        DynamicMultiSelectList partiesToReactivate = pcsCase.getPartiesToReactivate();

        List<UUID> uuidsToReactivate = partiesToReactivate.getValue().stream()
            .map(DynamicListElement::getCode)
            .toList();

        partyRepository.setPartiesActive(uuidsToReactivate, true);
    }


}
