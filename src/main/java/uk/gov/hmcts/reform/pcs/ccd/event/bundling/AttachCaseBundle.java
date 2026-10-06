package uk.gov.hmcts.reform.pcs.ccd.event.bundling;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.attachCaseBundle;

/**
 * Attaches a generated case bundle to its case. pcs-api uploads the bundle to CDAM itself, but
 * CDAM only lets ccd_data attach documents, and an unattached document expires (CDAM's TTL) and is
 * not checked against case access. Submitting the document through this event makes the data
 * store attach it, and leaves an entry in the case history for each generated bundle. The bundle
 * is recorded by CaseBundlePublisher, so there is nothing to persist here.
 */
@Component
public class AttachCaseBundle implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
            .decentralisedEvent(attachCaseBundle.name(), this::submit)
            .forAllStates()
            .name("Case bundle generated")
            .description("A case bundle was generated from the documents on the case")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRU, UserRole.SYSTEM_USER)
            .grantHistoryOnly(CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES))
            .page("attachCaseBundle")
            .optional(PCSCase::getCaseBundleAttachment);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }
}
