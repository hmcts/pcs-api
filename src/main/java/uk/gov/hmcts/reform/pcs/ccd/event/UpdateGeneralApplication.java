package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class UpdateGeneralApplication implements CCDConfig<GACase, State, UserRole> {

    private final GeneralApplicationService gaService;

    public UpdateGeneralApplication(GeneralApplicationService gaService) {
        this.gaService = gaService;
    }

    @Override
    public void configure(ConfigBuilder<GACase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.updateGeneralApplication.name(), this::aboutToSubmit)
            .forStateTransition(State.DRAFT, State.DRAFT_WITHDRAWN)
            .name("Withdraw Draft Gen App")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
            .fields()
            .page("Delete draft general application")
            .pageLabel("Are you sure you want to withdraw this draft application?")
            .label("lineSeparator", "---")
            .readonly(GACase::getStatus, NEVER_SHOW)
            .done();
    }

    private void aboutToSubmit(EventPayload<GACase, State> payload) {
        GACaseEntity toUpdate = gaService.findByCaseReference(payload.caseReference());
        toUpdate.setStatus(State.DRAFT_WITHDRAWN);
        gaService.saveGaApp(toUpdate);
    }

}
