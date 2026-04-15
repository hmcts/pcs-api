package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenCreateGenApp;

@Slf4j
@Component
@AllArgsConstructor
public class CitizenCreateGenApp implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final GenAppRepository genAppRepository;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(citizenCreateGenApp.name(), this::submit)
            .forAllStates() // TODO: Adjust once target states are known and available
            .name("Create a General Application")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRUD, UserRole.DEFENDANT)
            .showSummary();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        UUID currentUserId = securityContextService.getCurrentUserId();
        PartyEntity applicantParty = partyService.getPartyEntityByIdamId(currentUserId, caseReference);

        CitizenGenAppRequest citizenCreateGenApp = caseData.getCitizenGenAppRequest();

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(citizenCreateGenApp.getApplicationType())
            .party(applicantParty)
            .state(GenAppState.SUBMITTED)
            .build();

        pcsCaseEntity.addGenApp(genAppEntity);

        genAppRepository.save(genAppEntity);


        return SubmitResponse.<State>builder()
            .build();
    }

}
