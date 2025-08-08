package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.MakeAClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestApplication;

@Component
@AllArgsConstructor
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final MakeAClaim makeAClaim;
    private final PropertyNotEligible propertyNotEligible;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestApplication.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(makeAClaim)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new ClaimantInformation());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setApplicantForename("Preset value");
        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        pcsCaseService.createCase(caseReference, pcsCase);
    }

}
