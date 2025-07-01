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
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CoreCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

@Component
public class WithdrawGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseRepository pcsRepository;
    private final GeneralApplicationRepository gaRepository;
    private final GeneralApplicationService gaService;
    private final CoreCaseDataService coreCaseDataService;

    public WithdrawGeneralApplication(PCSCaseRepository pcsRepository,
                                      GeneralApplicationRepository gaRepository,
                                      GeneralApplicationService gaService, CoreCaseDataService coreCaseDataService) {
        this.pcsRepository = pcsRepository;
        this.gaRepository = gaRepository;
        this.gaService = gaService;
        this.coreCaseDataService = coreCaseDataService;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.withdrawGeneralApplication.name(), this::aboutToSubmit)
                .forAllStates()
                .name("Withdraw Draft Gen App")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .fields()
                .page("Withdraw draft general application")
                .pageLabel("Are you sure you want to withdraw this draft application?")
                .label("lineSeparator", "---")
                .done();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> payload) {
        PCSCase pcsCase = payload.caseData();

        String genAppRef = payload.urlParams().getFirst("genAppId");

        GACaseEntity toUpdate = gaRepository.findByCaseReference(Long.valueOf(genAppRef)).get();

        //Map<String, Object> existingCase = gaService.getCase(toUpdate.getCaseReference().toString()).getData();
        //existingCase.put("status", State.Withdrawn.toString());

        GACase gaData =
                GACase.builder().adjustment(toUpdate.getAdjustment())
                        .additionalInformation(toUpdate.getAdditionalInformation())
                    .caseReference(toUpdate.getCaseReference())
                    .status(State.WITHDRAWN).build();

        gaService.updateGeneralApplicationInCCD(
                toUpdate.getCaseReference().toString(),
                EventId.updateGeneralApplication.name(),
                gaData
        );

    }


}
