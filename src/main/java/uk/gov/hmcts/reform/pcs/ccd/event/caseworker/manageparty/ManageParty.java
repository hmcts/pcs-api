package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyStates;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.AddLitigationParty;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.AddPartyDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.ManagePartyOptionsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.UpdatePartyDetailsPage;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageParties;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@Component
public class ManageParty implements CCDConfig<PCSCase, State, UserRole> {

    private final StartEventHandler startEventHandler;
    private final SubmitEventHandler submitEventHandler;
    private final AddPartyDetailsPage addPartyDetailsPage;
    private final ManagePartyOptionsPage managePartyOptionsPage;
    private final UpdatePartyDetailsPage updatePartyDetailsPage;

    public ManageParty(@Qualifier("managePartyStartEventHandler") StartEventHandler startEventHandler,
                        @Qualifier("managePartySubmitEventHandler") SubmitEventHandler submitEventHandler,
                        AddPartyDetailsPage addPartyDetailsPage,
                        ManagePartyOptionsPage managePartyOptionsPage,
                        UpdatePartyDetailsPage updatePartyDetailsPage) {
        this.startEventHandler = startEventHandler;
        this.submitEventHandler = submitEventHandler;
        this.addPartyDetailsPage = addPartyDetailsPage;
        this.managePartyOptionsPage = managePartyOptionsPage;
        this.updatePartyDetailsPage = updatePartyDetailsPage;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(manageParties.name(), submitEventHandler, startEventHandler)
            .forStates(ManagePartyStates.ALLOWED_STATES)
            .name("Manage parties")
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_3, CASEWORKER_EVENTS))
            .showSummary()
            .endButtonLabel("Submit");

        new PageBuilder(eventBuilder)
            .add(managePartyOptionsPage)
            .add(new AddLitigationParty())
            .add(addPartyDetailsPage)
            .add(updatePartyDetailsPage);
    }

}
