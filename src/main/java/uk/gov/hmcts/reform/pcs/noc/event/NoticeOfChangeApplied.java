package uk.gov.hmcts.reform.pcs.noc.event;

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

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChangeApplied;

/**
 * System event submitted once a Notice of Change has been applied to the pcs tables. It changes no
 * data itself: the runtime stores a fresh case snapshot for it, which is what carries the re-derived
 * CaseAccessGroups into the search index so the incoming organisation's case list picks the case up.
 * Only the system user can trigger it; everyone else just sees the entry in the case history.
 */
@Component
@Slf4j
public class NoticeOfChangeApplied implements CCDConfig<PCSCase, State, UserRole> {

    private static final UserRole[] PARTY_ROLES = {
        UserRole.CLAIMANT,
        UserRole.PCS_SOLICITOR,
        UserRole.GA_CLAIMANT_SOLICITOR,
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR,
        UserRole.CITIZEN,
        UserRole.PCS_CASE_WORKER
    };

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(noticeOfChangeApplied.name(), this::submit)
            .forAllStates()
            .name("Notice of change applied")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRU, UserRole.SYSTEM_USER)
            .grantHistoryOnly(PARTY_ROLES)
            .grantHistoryOnly(CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Notice of change applied recorded for case {}", eventPayload.caseReference());
        return SubmitResponse.<State>builder().build();
    }
}
