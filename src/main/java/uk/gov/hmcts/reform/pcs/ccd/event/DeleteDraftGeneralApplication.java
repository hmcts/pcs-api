package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

@Component
public class DeleteDraftGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseRepository pcsRepository;
    private final GeneralApplicationRepository gaRepository;
    private final GeneralApplicationService gaService;

    public DeleteDraftGeneralApplication(PCSCaseRepository pcsRepository,
                                         GeneralApplicationRepository gaRepository,
                                         GeneralApplicationService gaService) {
        this.pcsRepository = pcsRepository;
        this.gaRepository = gaRepository;
        this.gaService = gaService;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent("deleteDraftGeneralApplication", this::aboutToSubmit, this::start)
            .forAllStates()
            //.forStates(State.Submitted, State.Open)
            .name("Delete Draft Gen App")
            .description("Delete Draft Gen App")
            //.showSummary() shows summary of event before the user submits, good for edit gen app event
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Delete draft general application")
            .pageLabel("Are you sure you want to delete this draft application?")
            .label("lineSeparator", "---")
            .readonly(PCSCase::getGeneralApplicationToDelete, "[STATE]=\"NEVER_SHOW\"")
            .done();
    }

    private PCSCase start(EventPayload<PCSCase, State> payload) {
        String genAppIdParam = payload.urlParams().getFirst("genAppId");
        if (genAppIdParam != null) {
            GeneralApplication toDelete = new GeneralApplication();
            toDelete.setApplicationId(genAppIdParam);
            payload.caseData().setGeneralApplicationToDelete(toDelete);
        }
        return payload.caseData();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> payload) {
        PCSCase pcsCase = payload.caseData();
        String genAppIdToDelete = pcsCase.getGeneralApplicationToDelete().getApplicationId();
        long caseReference = payload.caseReference();

        if (genAppIdToDelete == null) {
            throw new IllegalStateException("ID IS null");
        }
        gaRepository.deleteByApplicationId(genAppIdToDelete);
        PCS parentCase = pcsRepository.findByCcdCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("Parent case not found"));
        if (parentCase.getGeneralApplications() != null) {
            parentCase.getGeneralApplications().removeIf(ga -> genAppIdToDelete.equals(ga.getApplicationId()));
        }
        pcsRepository.save(parentCase);
        if (pcsCase.getGeneralApplications() != null) {
            pcsCase.getGeneralApplications().removeIf(ga -> genAppIdToDelete.equals(ga.getValue().getApplicationId()));
        }
        pcsCase.setGeneralApplicationToDelete(null);
    }


}
