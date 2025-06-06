package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {
    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .event("createTestApplication")
            .initialState(State.Open)
            .name("Make a claim")
            .aboutToStartCallback(this::start)
            .aboutToSubmitCallback(this::aboutToSubmit)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Make a claim")
            .pageLabel("What is the address of the property you are claiming possession of?")
            .label("---", "---")
            .mandatory(PCSCase::getClaimPropertyAddress)
            .page("Additional information")
            .pageLabel("Please enter applicant's name")
                .mandatory(PCSCase::getApplicantForename)
                .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase data = caseDetails.getData();
        data.setApplicantForename("Preset value");

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> aboutToSubmit(CaseDetails<PCSCase, State> details,
                                                                       CaseDetails<PCSCase, State> beforeDetails) {
        // TODO: Whatever you need.
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }
}
