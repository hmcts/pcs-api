package uk.gov.hmcts.reform.pcs.ccd.event;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;


@Component
@Slf4j
@AllArgsConstructor
public class CreateFlags implements CCDConfig<PCSCase, State, UserRole> {

    private  static final String ALWAYS_HIDE = "flagLauncherInternal = \"ALWAYS_HIDE\"";
    private  final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(EventId.createFlags.name(), this::submit)
                .forAllStates()
                .name("Create flags")
                .description("To create flags")
                .showSummary()
                .grant(Permission.CRUD,UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .page("caseFlags")
            .pageLabel("Case Flags")
            .optional(PCSCase::getCaseFlags,ALWAYS_HIDE,true,true)
            .optional(PCSCase::getParties,ALWAYS_HIDE,true,true)
            .list(PCSCase::getParties, ALWAYS_HIDE)
                .optional(Party::getFirstName, ALWAYS_HIDE)
                .optional(Party::getLastName, ALWAYS_HIDE)
                .optional(Party::getOrgName, ALWAYS_HIDE)
                .optional(Party::getNameKnown, ALWAYS_HIDE)
                .optional(Party::getEmailAddress, ALWAYS_HIDE)
                .complex(Party::getAddress, ALWAYS_HIDE)
                .done()
                .optional(Party::getAppellantFlags, ALWAYS_HIDE,  true)
                .done()
            .list(PCSCase::getParties, ALWAYS_HIDE)
                .optional(Party::getFirstName, ALWAYS_HIDE)
                .optional(Party::getLastName, ALWAYS_HIDE)
                .optional(Party::getRespondentFlags, ALWAYS_HIDE,  true)
            .done()
            .optional(PCSCase::getFlagLauncherInternal,
                 null,null,null,null,"#ARGUMENT(CREATE)")
        ;

    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Caseworker created case link for {}", caseReference);

        pcsCaseService.patchCaseFlags(caseReference, pcsCase);

        return SubmitResponse.defaultResponse();
    }
}
