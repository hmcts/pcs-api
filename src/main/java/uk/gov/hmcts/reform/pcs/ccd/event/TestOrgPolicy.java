package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.testOrgPolicy;

@Slf4j
@Component
public class TestOrgPolicy implements CCDConfig<PCSCase, State, UserRole> {

    public static OrganisationPolicy<UserRole> organisationPolicy = null;

    private static final String DEFAULT_ROLE = "[CREATOR]";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(testOrgPolicy.name(), this::submit, this::start)
            .forAllStates()
            .name("Test Org Policy")
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .showSummary()
            .fields()
            .page("orgPolicy")
            .complex(PCSCase::getOrganisationPolicy)
                .complex(OrganisationPolicy::getOrganisation)
                    .mandatory(Organisation::getOrganisationId)
                    .mandatoryNoSummary(Organisation::getOrganisationName)
                .done()
                .optional(OrganisationPolicy::getOrgPolicyCaseAssignedRole, null, DEFAULT_ROLE)
                .optional(OrganisationPolicy::getOrgPolicyReference)
            .done();
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase casedData = eventPayload.caseData();
        casedData.setOrganisationPolicy(OrganisationPolicy.<UserRole>builder()
                                            .prepopulateToUsersOrganisation(YesOrNo.YES)
                                            .build());
        return casedData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        organisationPolicy = caseData.getOrganisationPolicy();

        return SubmitResponse.defaultResponse();
    }

}
