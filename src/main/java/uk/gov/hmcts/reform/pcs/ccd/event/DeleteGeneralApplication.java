package uk.gov.hmcts.reform.pcs.ccd.event;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

import java.util.UUID;

public class DeleteGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {


    private final PCSCaseRepository pcsRepository;

    private final GeneralApplicationRepository gaRepository;

    private final GeneralApplicationService gaService;

    public DeleteGeneralApplication(PCSCaseRepository pcsRepository,
                                    GeneralApplicationRepository gaRepository,
                                    GeneralApplicationService gaService) {
        this.pcsRepository = pcsRepository;
        this.gaRepository = gaRepository;
        this.gaService = gaService;
    }


    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent("removeGeneralApplication", this::aboutToSubmit)
            .forStates(State.Submitted, State.Open)
            .name("Remove General Application from Case")
            .description("Remove a draft general application from this case")
            .showSummary()
            .grant(Permission.D, UserRole.CASE_WORKER)
            .fields()
            .page("Remove General Application")
            .mandatory(PCSCase::getGeneralApplicationToDelete)
            .done();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> payload) {
        PCSCase pcsCase = payload.caseData();
        UUID gaIdToDelete = pcsCase.getGeneralApplicationToDelete().getId();


        gaRepository.deleteById(gaIdToDelete);

        // Optionally, remove from in-memory list if you use it in the DTO
        if (pcsCase.getGeneralApplications() != null) {
            pcsCase.getGeneralApplications().removeIf(ga -> gaIdToDelete.equals(ga.getId()));
        }

        pcsCase.setGeneralApplicationToDelete(null);
    }


}
