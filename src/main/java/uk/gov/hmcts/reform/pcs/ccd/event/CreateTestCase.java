package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.SneakyThrows;
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
            .name("Create test case")
            .aboutToStartCallback(this::start)
            .aboutToSubmitCallback(this::aboutToSubmit)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Create test case")
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

    @SneakyThrows
    public AboutToStartOrSubmitResponse<PCSCase, State> aboutToSubmit(CaseDetails<PCSCase, State> details,
                                                                       CaseDetails<PCSCase, State> beforeDetails) {


        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }
}
