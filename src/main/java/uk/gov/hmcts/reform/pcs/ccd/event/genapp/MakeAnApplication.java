package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.AppliedForHelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.ChooseAnApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.DocumentUploadWanted;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.HearingInNext14Days;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.HelpWithFeesNeeded;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.MustApplyForHelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.OtherPartiesAgreed;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.ReasonsNotToShare;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.SelectParty;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StartAdjourn;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StartSetAside;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StartSomethingElse;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.UploadSupportingDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.WhatOrderWanted;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.WhichLanguage;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.makeAnApplication;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@Slf4j
@Component
public class MakeAnApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final StartEventHandler startEventHandler;
    private final SubmitEventHandler submitEventHandler;

    public MakeAnApplication(@Qualifier("genAppStartEventHandler") StartEventHandler startEventHandler,
                             @Qualifier("genAppSubmitEventHandler") SubmitEventHandler submitEventHandler) {
        this.startEventHandler = startEventHandler;
        this.submitEventHandler = submitEventHandler;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(makeAnApplication.name(), submitEventHandler, startEventHandler)
            .forAllStates() // TODO: Adjust once target states are known and available
            .name("Make an application")
            .grant(Permission.CRUD, UserRole.DEFENDANT)
            .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Submit")
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ChooseAnApplication())
            .add(new StartAdjourn())
            .add(new StartSetAside())
            .add(new StartSomethingElse())
            .add(new SelectParty())
            .add(new HearingInNext14Days())
            .add(new HelpWithFeesNeeded())
            .add(new AppliedForHelpWithFees())
            .add(new MustApplyForHelpWithFees())
            .add(new OtherPartiesAgreed())
            .add(new ReasonsNotToShare())
            .add(new WhatOrderWanted())
            .add(new DocumentUploadWanted())
            .add(new UploadSupportingDocuments())
            .add(new WhichLanguage())
            .add(new StatementOfTruth());
    }

}
