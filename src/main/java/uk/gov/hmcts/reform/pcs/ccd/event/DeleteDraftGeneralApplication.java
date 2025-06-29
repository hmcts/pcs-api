package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

@Component
public class DeleteDraftGeneralApplication implements CCDConfig<GACase, State, UserRole> {

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
    public void configure(ConfigBuilder<GACase, State, UserRole> builder) {
        builder.decentralisedEvent("deleteDraftGeneralApplication", this::aboutToSubmit)
            .forStateTransition(State.Draft, State.Withdrawn)
            .name("Delete Draft Gen App")
            .description("Delete Draft Gen App")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Delete draft general application")
            .pageLabel("Are you sure you want to delete this draft application?")
            .label("lineSeparator", "---")
            .done();
    }

    private GACase aboutToSubmit(EventPayload<GACase, State> payload) {
        GACase genApp = payload.caseData();

        long caseReference = payload.caseReference();

        if (genApp.getCaseReference() == null) {
            throw new IllegalStateException("ID IS null");
        }
        genApp.setStatus(State.Withdrawn);
        gaRepository.deleteByCaseReference(genApp.getCaseReference());
        return genApp;
    }


}
