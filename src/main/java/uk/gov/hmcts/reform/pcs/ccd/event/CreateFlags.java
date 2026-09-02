package uk.gov.hmcts.reform.pcs.ccd.event;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@Component
@Slf4j
@AllArgsConstructor
public class CreateFlags implements CCDConfig<PCSCase, State, UserRole> {

    /**
     * Case flags may also be created before the case is issued. The shared
     * {@link EventStates#createFlags()} set is not extended for this, because Make an application shares
     * it and is only available once the case has been issued.
     */
    static final State[] CREATE_FLAG_STATES =
        ArrayUtils.addAll(EventStates.createFlags(), State.PENDING_CASE_ISSUED);

    private  final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                .decentralisedEvent(EventId.createFlags.name(), this::submit)
                .forStates(CREATE_FLAG_STATES)
                .name("Create case flags")
                .description("To create flags")
                .showSummary()
                .endButtonLabel("Submit")
                .grant(Permission.CRU,
                       UserRole.CTSC_ADMIN,
                       UserRole.HEARING_CENTRE_ADMIN,
                       UserRole.WLU_ADMIN)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES))
                .page("caseworkerCaseFlag")
                .pageLabel("Create case flags")
                .label("caseworkerCaseFlag-lineSeparator", "---")
                .optional(PCSCase::getCaseFlags, ShowConditions.NEVER_SHOW, true, true)
                .optional(PCSCase::getParties, ShowConditions.NEVER_SHOW, true, true)
                .list(PCSCase::getAllDefendants, ShowConditions.NEVER_SHOW)
                    .optional(Party::getDefendantFlags, ShowConditions.NEVER_SHOW, true)
                    .optional(Party::getPartyFlagsExternal, ShowConditions.NEVER_SHOW, true)
                .done()
                .optional(PCSCase::getFlagLauncherInternal,
                      null, null, null, null,
                      "#ARGUMENT(CREATE,VERSION2.1)");

    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.debug("Caseworker created case flag for {}", caseReference);

        pcsCaseService.patchCaseFlags(caseReference, pcsCase);

        return SubmitResponse.defaultResponse();
    }
}
