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
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_CASE_ISSUED;


@Component
@Slf4j
@AllArgsConstructor
public class ManageFlags implements CCDConfig<PCSCase, State, UserRole> {
    private  final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                            .decentralisedEvent(EventId.amendFlags.name(), this::submit)
                            .forStates(CASE_ISSUED, PENDING_CASE_ISSUED)
                            .name("Manage Flags")
                            .description("To manage flags")
                            .showSummary()
                            .grant(Permission.CRU, UserRole.PCS_CASE_WORKER))
            .page("caseworkerCaseFlag")
            .pageLabel("Case Flags")
            .optional(PCSCase::getCaseFlags, ShowConditions.NEVER_SHOW, true, true)
            .optional(PCSCase::getAllDefendants, ShowConditions.NEVER_SHOW, true, true)
            .page("respondentFlags")
            .list(PCSCase::getParties, ShowConditions.NEVER_SHOW)
                .optional(Party::getRespondentFlags, ShowConditions.NEVER_SHOW,  true)
            .done()
            .optional(PCSCase::getFlagLauncherInternal,null, null,
                null, null, "#ARGUMENT(UPDATE)");
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.debug("Caseworker updated case flag for {}", caseReference);

        pcsCaseService.patchCaseFlags(caseReference, pcsCase);

        return SubmitResponse.defaultResponse();
    }
}
