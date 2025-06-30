package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.GA;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

@Component
public class UpdateGeneralApplication implements CCDConfig<GACase, State, UserRole> {

    private final PCSCaseRepository pcsRepository;
    private final GeneralApplicationRepository gaRepository;
    private final GeneralApplicationService gaService;

    public UpdateGeneralApplication(PCSCaseRepository pcsRepository,
                                      GeneralApplicationRepository gaRepository,
                                      GeneralApplicationService gaService) {
        this.pcsRepository = pcsRepository;
        this.gaRepository = gaRepository;
        this.gaService = gaService;
    }

    @Override
    public void configure(ConfigBuilder<GACase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.updateGeneralApplication.name(), this::aboutToSubmit)
            .forStateTransition(State.Draft, State.Withdrawn)
            .name("Withdraw Draft Gen App")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Delete draft general application")
            .pageLabel("Are you sure you want to withdraw this draft application?")
            .label("lineSeparator", "---")
            .readonly(GACase::getStatus, "[STATE]=\"NEVER_SHOW\"")
            .done();
    }

    private void aboutToSubmit(EventPayload<GACase, State> payload) {
        GA toUpdate = gaRepository.findByCaseReference(payload.caseReference()).get();
        toUpdate.setStatus(State.Withdrawn);
        gaRepository.save(toUpdate);
    }

}




