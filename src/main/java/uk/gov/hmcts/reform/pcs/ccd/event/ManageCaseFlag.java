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
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

@Component
@Slf4j
@Setter
@AllArgsConstructor
public class ManageCaseFlag implements CCDConfig<PCSCase, State, UserRole> {

    private static final String ALWAYS_HIDE = "flagLauncher = \"ALWAYS_HIDE\"";
    private final PcsCaseService pcsCaseService;


    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
            .decentralisedEvent("manageFlags", this::submit)
            .forAllStates()
            .name("Manage Flag")
            .description("Manage Flag")
            .showSummary()
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR))
            .page("manageCaseFlag")
            .pageLabel("Manage Case Flags")
            .optional(PCSCase::getCaseFlags, ALWAYS_HIDE, true, true)
            .optional(PCSCase::getParties, ALWAYS_HIDE, true, true)
            .list(PCSCase::getParties, ALWAYS_HIDE)
                .optional(Party::getFirstName, ALWAYS_HIDE)
                .optional(Party::getLastName, ALWAYS_HIDE)
                .optional(Party::getOrgName, ALWAYS_HIDE)
                .optional(Party::getNameKnown, ALWAYS_HIDE)
                .optional(Party::getEmailAddress, ALWAYS_HIDE)
                .complex(Party::getAddress, ALWAYS_HIDE)
                .done()
                .optional(Party::getAddressKnown, ALWAYS_HIDE)
                .optional(Party::getAddressSameAsProperty, ALWAYS_HIDE)
                .optional(Party::getPhoneNumber, ALWAYS_HIDE)
                .optional(Party::getPhoneNumberProvided, ALWAYS_HIDE)
                .optional(Party::getFlags, ALWAYS_HIDE, true)
            .done()
            .optional(
                PCSCase::getFlagLauncher,
                null, null, null, null, "#ARGUMENT(UPDATE,VERSION2.1)");
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        pcsCaseService.updateCaseFlags(caseReference, pcsCase.getCaseFlags(), pcsCase.getParties());
        log.info("Manage case flag Submitted {}", pcsCase);
        return SubmitResponse.defaultResponse();
    }

}
