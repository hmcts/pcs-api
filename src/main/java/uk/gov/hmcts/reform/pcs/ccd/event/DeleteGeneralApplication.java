package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class DeleteGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseService pcsCaseService;
    private final GeneralApplicationService gaService;

    public DeleteGeneralApplication(PCSCaseService pcsCaseService, GeneralApplicationService gaService) {
        this.pcsCaseService = pcsCaseService;
        this.gaService = gaService;;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.deleteGeneralApplication.name(), this::aboutToSubmit)
            .forAllStates()
            .showCondition(NEVER_SHOW)
            .name("Delete Draft Gen App")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
            .fields()
            .page("Delete draft general application")
            .pageLabel("Are you sure you want to Delete this draft application?")
            .label("lineSeparator", "---")
            .done();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> payload) {
        PCSCase pcsCase = payload.caseData();
        PcsCaseEntity pcs = pcsCaseService.findPCSCase(pcsCase.getCaseReference());
        String genAppRef = payload.urlParams().getFirst("genAppId").replace("-", "");
        Long genAppId = Long.parseLong(genAppRef);
        List<ListValue<GACase>> gaList = pcsCase.getGeneralApplications();
        List<ListValue<GACase>> updatedList = gaList.stream()
            .filter(gaListValue ->
                        !genAppId.equals(gaListValue.getValue().getCaseReference())
            )
            .collect(Collectors.toList());
        pcsCase.setGeneralApplications(updatedList);

        pcs.getGeneralApplications().removeIf(ga -> genAppId.equals(ga.getCaseReference()));
        pcsCaseService.savePCSCase(pcs);
        gaService.deleteGenApp(genAppId);
    }

}
