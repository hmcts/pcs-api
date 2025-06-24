package uk.gov.hmcts.reform.pcs.ccd.event;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

import java.util.List;

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
            .initialState(State.Draft)
            .name("Remove General Application from Case")
            .grant(Permission.D, UserRole.CASE_WORKER);

    }

    private void deleteGA(EventPayload<PCSCase, State> payload) {
        List<GeneralApplication> list = payload.caseData().getGeneralApplicationList();
        long gaId = 1L;

        gaService.deleteDraft(gaId);
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> pcsCaseStateEventPayload) {

        /**
        PCSCase pcsCase = pcsCaseStateEventPayload.caseData();

        // Get GA id to remove from event data/context
        String applicationIdToRemove = getGaIdToRemoveFromContext();

        if (applicationIdToRemove != null && pcsCase.getGeneralApplicationList() != null) {
            pcsCase.getGeneralApplicationList().removeIf(ga -> applicationIdToRemove.equals(ga.getId()));

            // Delete GA draft or case from your DB
            gaRepository.deleteById(applicationIdToRemove);

            // Save updated PCS case
            pcsRepository(pcsCase);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(pcsCase)
            .build();
         */
    }


}
