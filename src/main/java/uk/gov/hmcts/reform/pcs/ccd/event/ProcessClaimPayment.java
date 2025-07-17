package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
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
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.processClaimPayment;

@Component
@Slf4j
@AllArgsConstructor
public class ProcessClaimPayment implements CCDConfig<PCSCase, State, UserRole> {

    private final PostCodeCourtService postCodeCourtService;
    private final PcsCaseService pcsCaseService;


    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
            .decentralisedEvent(processClaimPayment.name(), this::submit)
            .forStates(CASE_ISSUED)
            .showCondition(NEVER_SHOW)
            .name("Claim Payment")
                .grant(Permission.CRU, PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new ClaimPayment());
    }

    private void submit(EventPayload<PCSCase, State> payload) {

        PCSCase pcsCase = payload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.PAID);
        Integer epimId = getCourtManagementLocation(pcsCase.getPropertyAddress().getPostCode());
        pcsCase.setCaseManagementLocation(epimId);
        pcsCaseService.patchCase(payload.caseReference(), pcsCase);

    }

    private Integer getCourtManagementLocation(String postCode) {

        List<Court> results = postCodeCourtService.getCountyCourtsByPostCode(
            postCode);

        if (results.isEmpty()) {
            log.error("Case management location couldn't be allocated for postcode: {}", postCode);
            return null;
        }
        log.info("Case management venue allocation found for PostCode {}", postCode);
        return results.getFirst().epimId();
    }
}
