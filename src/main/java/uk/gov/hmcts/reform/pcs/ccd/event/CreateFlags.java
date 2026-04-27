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
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;


@Component
@Slf4j
@AllArgsConstructor
public class CreateFlags implements CCDConfig<PCSCase, State, UserRole> {

    private  final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                .decentralisedEvent(EventId.createFlags.name(), this::submit)
                .forAllStates()
                .name("Create case flags")
                .description("To create flags")
                .showSummary()
                .grant(Permission.CRU, UserRole.PCS_CASE_WORKER))
                .page("caseworkerCaseFlag")
                .pageLabel("Case Flags")
                .optional(PCSCase::getCaseFlags, ShowConditions.NEVER_SHOW, true, true)
                .optional(
                PCSCase::getFlagLauncherInternal,
                null, null, null, null,
                "#ARGUMENT(CREATE)"
            );

    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.debug("Caseworker created case flag for {}", caseReference);

        pcsCaseService.patchCaseFlags(caseReference, pcsCase);

        return SubmitResponse.defaultResponse();
    }
}

