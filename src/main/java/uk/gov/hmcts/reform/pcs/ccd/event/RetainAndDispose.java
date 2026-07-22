package uk.gov.hmcts.reform.pcs.ccd.event;

import static uk.gov.hmcts.ccd.sdk.RetainAndDisposePolicy.CONFIRM_DISPOSAL_EVENT_ID;
import static uk.gov.hmcts.ccd.sdk.RetainAndDisposePolicy.DISPOSAL_EVENT_ID;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.SYSTEM_USER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PendingDisposal;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
public class RetainAndDispose implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .event(DISPOSAL_EVENT_ID)
            .forStateTransition(AWAITING_SUBMISSION_TO_HMCTS, PendingDisposal)
            .name("Mark draft for disposal")
            .description("Move an inactive draft into its disposal state")
            .grant(Permission.CRU, SYSTEM_USER);

        configBuilder
            .event(CONFIRM_DISPOSAL_EVENT_ID)
            .forStateTransition(PendingDisposal, PendingDisposal)
            .ttlIncrement(0)
            .name("Confirm draft disposal")
            .description("Set the disposal TTL after verifying the case is readable")
            .grant(Permission.CRU, SYSTEM_USER);
    }
}
