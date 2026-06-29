package uk.gov.hmcts.reform.pcs.ccd.event;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

@Component
@Slf4j
@AllArgsConstructor
public class MaintainLinkCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                            .decentralisedEvent(EventId.maintainCaseLink.name(), this::submit)
                            .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
                            .name("Manage case links")
                            .description("To manage link related cases")
                            .grant(Permission.CRUD, UserRole.CTSC_ADMIN, UserRole.HEARING_CENTRE_ADMIN,
                                   UserRole.CIRCUIT_JUDGE, UserRole.FEE_PAID_JUDGE,
                                   UserRole.JUDGE, UserRole.LEADERSHIP_JUDGE))
            .page("maintainCaseLink")
            .pageLabel("Case Link")
            .optional(PCSCase::getCaseLinks, "LinkedCasesComponentLauncher = \"DONOTSHOW\"", null, true)
            .optional(PCSCase::getLinkedCasesComponentLauncher,
                      null, null, null, null, "#ARGUMENT(UPDATE,LinkedCases)");
    }


    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Caseworker updated case link for {}", caseReference);

        pcsCaseService.patchCaseLinks(caseReference, pcsCase);

        return SubmitResponse.defaultResponse();
    }


}
