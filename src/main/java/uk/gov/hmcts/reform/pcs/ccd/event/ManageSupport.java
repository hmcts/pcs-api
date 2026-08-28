package uk.gov.hmcts.reform.pcs.ccd.event;

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
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagHistoryRoles.EXTERNAL_CASE_FLAG_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventStates.manageSupport;

@Component
@Slf4j
@AllArgsConstructor
public class ManageSupport implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                .decentralisedEvent(EventId.manageSupport.name(), this::submit)
                .forStates(manageSupport())
                .name("Manage support")
                .description("To manage support")
                .showSummary()
                .endButtonLabel("Submit")
                .grant(Permission.CRU, EXTERNAL_CASE_FLAG_ROLES)
                .grantHistoryOnly(EXTERNAL_CASE_FLAG_HISTORY_ROLES))
                .page("externalCaseFlag")
                .pageLabel("Manage support")
                .optional(PCSCase::getPartySupport, ShowConditions.NEVER_SHOW, true, true)
                .list(PCSCase::getPartySupport, ShowConditions.NEVER_SHOW)
                    .optional(PartySupport::getSupportFlags, ShowConditions.NEVER_SHOW, true)
                .done()
                .optional(PCSCase::getFlagLauncherExternal,
                      null, null, null, null,
                      "#ARGUMENT(UPDATE,EXTERNAL)");
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.debug("External user updated support for {}", caseReference);

        pcsCaseService.patchSupportFlags(caseReference, pcsCase.getPartySupport());

        return SubmitResponse.defaultResponse();
    }
}
