package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasenote.AddCaseNoteConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNoteService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.addCaseNote;

@Component
@AllArgsConstructor
public class AddCaseNote implements CCDConfig<PCSCase, State, UserRole> {

    private final AddCaseNoteConfigurer addCaseNoteConfigurer;
    private final CaseNoteService caseNoteService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(addCaseNote.name(), this::submit)
                        .forAllStates()
                        .name("Add a case note")
                        .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                        .showSummary();
        addCaseNoteConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {

        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        caseNoteService.addCaseNote(caseReference, pcsCase);
        return SubmitResponse.defaultResponse();
    }

}
