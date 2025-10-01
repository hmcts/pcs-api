package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class EditParty implements CCDConfig<PCSCase, State, UserRole> {

    private final PartyRepository partyRepository;

    public EditParty(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.editParty.name(), this::submit, this::start)
            .forAllStates()
            .name("Edit party")
            .showSummary()
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .showCondition(NEVER_SHOW)
            .fields()
            .page("party")
            .mandatory(PCSCase::getCurrentParty)
            .done();
    }


    private PCSCase start(EventPayload<PCSCase, State> payload) {
        UUID partyId = UUID.fromString(payload.urlParams().getFirst("partyId"));

        uk.gov.hmcts.reform.pcs.entity.Party partyEntity = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("Party not found for ID " + partyId));

        Party party = Party.builder()
            .id(partyEntity.getId())
            .forename(partyEntity.getForename())
            .surname(partyEntity.getSurname())
            .active(YesOrNo.from(partyEntity.getActive()))
            .build();

        PCSCase pcsCase = payload.caseData();
        pcsCase.setCurrentParty(party);
        return pcsCase;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {

        PCSCase pcsCase = eventPayload.caseData();

        Party partyToUpdate = pcsCase.getCurrentParty();
        UUID partyId = partyToUpdate.getId();

        uk.gov.hmcts.reform.pcs.entity.Party partyEntity = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("Party not found for ID " + partyId));

        partyEntity.setForename(partyToUpdate.getForename());
        partyEntity.setSurname(partyToUpdate.getSurname());
        partyEntity.setActive(partyToUpdate.getActive().toBoolean());

        partyRepository.save(partyEntity);
    }

}
