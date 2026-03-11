package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.Setter;
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
@Setter
@AllArgsConstructor
public class CreateCaseFlag implements CCDConfig<PCSCase, State, UserRole> {

    private static final String ALWAYS_HIDE = "flagLauncher = \"ALWAYS_HIDE\"";
    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
            .decentralisedEvent("createFlags",this::submit)
            .forAllStates()
            .name("Create Flag")
            .description("Create Flag")
            .showSummary()
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR))
            .page("caseworkerCaseFlag")
            .pageLabel("Case Flags")
            .optional(PCSCase::getCaseFlags, ALWAYS_HIDE, true, true)
            .optionalList(PCSCase::getParties, ALWAYS_HIDE, true)
            .done()
            .optional(PCSCase::getFlagLauncher, null, null, null, null, "#ARGUMENT(CREATE,VERSION2.1)");
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Create case flag Submitted {}", pcsCase);
        return SubmitResponse.defaultResponse();
    }

}
