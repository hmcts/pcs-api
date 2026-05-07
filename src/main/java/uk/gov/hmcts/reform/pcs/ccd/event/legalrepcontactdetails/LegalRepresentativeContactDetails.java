package uk.gov.hmcts.reform.pcs.ccd.event.legalrepcontactdetails;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails.LegalRepresentativeContactDetailsPage;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepresentativeContactDetails;

@Slf4j
@Component
@AllArgsConstructor
public class LegalRepresentativeContactDetails implements CCDConfig<PCSCase, State, UserRole> {

    private final LegalRepresentativeContactDetailsPage legalRepresentativeContactDetailsPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(legalRepresentativeContactDetails.name(), this::submit, this::start)
                .forAllStates()
                .name("Update legal rep's details")
                .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(legalRepresentativeContactDetailsPage);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();

        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        return SubmitResponse.defaultResponse();
    }


}
