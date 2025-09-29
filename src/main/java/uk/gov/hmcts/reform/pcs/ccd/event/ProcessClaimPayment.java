package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.claimpayment.ClaimPayment;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.processClaimPayment;

@Component
@AllArgsConstructor
public class ProcessClaimPayment implements CCDConfig<PCSCase, State, UserRole> {

    private final PostCodeCourtService postCodeCourtService;
    private final PcsCaseService pcsCaseService;


    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
            .decentralisedEvent(processClaimPayment.name(), this::submit)
            .forStates(CASE_ISSUED)
            .showCondition(NEVER_SHOW)
            .name("Claim Payment")
            .grant(Permission.CRU, PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(new ClaimPayment());
    }

    private void submit(EventPayload<PCSCase, State> payload) {

        PCSCase pcsCase = payload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.PAID);
        Integer epimId = postCodeCourtService.getCourtManagementLocation(pcsCase.getPropertyAddress().getPostCode());
        pcsCase.setCaseManagementLocation(epimId);
        // Note: For the real payment implementation, we should make the
        // payments at the claim level rather than the case
        pcsCaseService.patchCase(payload.caseReference(), pcsCase);
    }

}
