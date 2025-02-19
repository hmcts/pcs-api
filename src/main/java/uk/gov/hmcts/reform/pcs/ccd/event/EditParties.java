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

import java.util.List;

@Component
public class EditParties implements CCDConfig<PCSCase, State, UserRole> {
    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .event("editParties")
            .forAllStates()
            .name("Edit parties")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Edit parties", this::validateParties)
            .mandatory(PCSCase::getParties)
            .done();
    }

    // Example mid event callback handler
    private AboutToStartOrSubmitResponse<PCSCase, State> validateParties(CaseDetails<PCSCase, State> details,
                                                                         CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase result = details.getData();
        var errors = List.<String>of();
        // Example validation
        if (result.getParties().stream().anyMatch(x -> x.getValue().getForename().contains("Bob"))) {
            errors = List.of("Bob is not allowed!");
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(result)
            .errors(errors)
            .build();
    }
}
