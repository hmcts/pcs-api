package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;

@Component
@AllArgsConstructor
@Slf4j
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(new ClaimantInformation());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        log.info("Callback for start");
        PCSCase caseData = eventPayload.caseData();
        caseData.setApplicantForename("Preset value");
        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Callback for submit");

        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        pcsCaseService.createCase(caseReference, pcsCase);
    }

}
