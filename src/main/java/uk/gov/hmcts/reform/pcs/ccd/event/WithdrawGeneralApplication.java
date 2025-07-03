package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class WithdrawGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final GeneralApplicationService gaService;

    public WithdrawGeneralApplication(GeneralApplicationService gaService) {
        this.gaService = gaService;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.withdrawGeneralApplication.name(), this::submit)
                .forAllStates()
                .showCondition(NEVER_SHOW)
                .name("Withdraw Draft Gen App")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .fields()
                .page("Withdraw draft general application")
                .pageLabel("Are you sure you want to withdraw this draft application?")
                .label("lineSeparator", "---")
                .done();
    }

    private void submit(EventPayload<PCSCase, State> payload) {

        String genAppRef = payload.urlParams().getFirst("genAppId").replace("-", "");

        GACaseEntity toUpdate = gaService.findByCaseReference(Long.valueOf(genAppRef));

        GACase gaData =
                GACase.builder().adjustment(toUpdate.getAdjustment())
                        .additionalInformation(toUpdate.getAdditionalInformation())
                        .caseReference(toUpdate.getCaseReference())
                        .status(State.DRAFT_WITHDRAWN).build();

        gaService.updateGeneralApplicationInCCD(
                toUpdate.getCaseReference().toString(),
                EventId.updateGeneralApplication.name(),
                gaData
        );
    }

}
