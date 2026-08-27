package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.ChooseAnApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.DocumentUploadWanted;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.HearingInNext14Days;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.OtherPartiesAgreed;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.ReasonsNotToShare;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.StartAdjourn;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.StartSetAside;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.StartSomethingElse;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.UploadSupportingDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.WhatOrderWanted;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.WhichLanguage;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimantMakeAnApplication;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventStates.claimantMakeAnApplication;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@Slf4j
@Component
public class ClaimantMakeAnApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final StartEventHandler startEventHandler;
    private final SubmitEventHandler submitEventHandler;
    private final StatementOfTruth statementOfTruth;

    public ClaimantMakeAnApplication(@Qualifier("genAppStartEventHandler") StartEventHandler startEventHandler,
                                     @Qualifier("genAppSubmitEventHandler") SubmitEventHandler submitEventHandler,
                                     StatementOfTruth statementOfTruth) {
        this.startEventHandler = startEventHandler;
        this.submitEventHandler = submitEventHandler;
        this.statementOfTruth = statementOfTruth;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(claimantMakeAnApplication.name(), submitEventHandler, startEventHandler)
            .forStates(claimantMakeAnApplication())
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_3))
            .name("Make an application")
            .grant(Permission.CRUD, UserRole.CLAIMANT_SOLICITOR)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Submit")
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ChooseAnApplication())
            .add(new StartAdjourn())
            .add(new StartSetAside())
            .add(new StartSomethingElse())
            .add(new HearingInNext14Days())
            .add(new OtherPartiesAgreed())
            .add(new ReasonsNotToShare())
            .add(new WhatOrderWanted())
            .add(new DocumentUploadWanted())
            .add(new UploadSupportingDocuments())
            .add(new WhichLanguage())
            .add(statementOfTruth);
    }

}
