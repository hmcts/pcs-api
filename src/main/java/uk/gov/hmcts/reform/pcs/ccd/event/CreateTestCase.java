package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.MakeAClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestApplication;

@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private ClaimantInformation claimantInformationPage;
    private long caseReference;

    public CreateTestCase(PcsCaseService pcsCaseService, ClaimantInformation claimantInformationPage) {
        this.pcsCaseService = pcsCaseService;
        this.claimantInformationPage = claimantInformationPage;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestApplication.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Make a claim")
                .showSummary()
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new MakeAClaim(pcsCaseService, caseReference))
            .add(claimantInformationPage);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        OrganisationPolicy<UserRole> organisationPolicy = new OrganisationPolicy<UserRole>();
        organisationPolicy.setPrepopulateToUsersOrganisation(YesOrNo.YES);
        organisationPolicy.setOrgPolicyCaseAssignedRole(UserRole.CREATOR);
        caseData.setOrganisationPolicy(organisationPolicy);

        caseData.setShortenedName(organisationPolicy.getOrgPolicyReference());

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
