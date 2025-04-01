package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.UUID;

@Component
public class EditParty implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyRepository partyRepository;

    public EditParty(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.editParty.name(), this::submit, this::start)
            .forAllStates()
            .name("Edit party")
            .showSummary()
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("party")
            .mandatory(PcsCase::getCurrentParty)
            .done();
    }


    private PcsCase start(EventPayload<PcsCase, State> payload) {
        UUID partyId = UUID.fromString(payload.urlParams().getFirst("partyId"));

        uk.gov.hmcts.reform.pcs.entity.Party partyEntity = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("Party not found for ID " + partyId));

        Party party = Party.builder()
            .id(partyEntity.getId())
            .forename(partyEntity.getForename())
            .surname(partyEntity.getSurname())
            .active(YesOrNo.from(partyEntity.getActive()))
            .build();

        PcsCase pcsCase = payload.caseData();
        pcsCase.setCurrentParty(party);
        return pcsCase;
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {

        PcsCase pcsCase = eventPayload.caseData();

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
