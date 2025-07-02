package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class AddGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final GeneralApplicationService genAppService;
    private final PCSCaseService pcsCaseService;

    public AddGeneralApplication(GeneralApplicationService genAppService, PCSCaseService pcsCaseService) {
        this.genAppService = genAppService;
        this.pcsCaseService = pcsCaseService;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.addGeneralApplication.name(), this::aboutToSubmit)
            .forStates(State.CASE_ISSUED)
            .name("Make General Application")
            .showSummary()
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
            .fields()
            .page("General Application Details")
            .complex(PCSCase::getCurrentGeneralApplication)
            .mandatory(GACase::getGaType)
            .mandatory(GACase::getAdjustment)
            .optional(GACase::getAdditionalInformation)
            .readonly(GACase::getCaseLink, NEVER_SHOW)
            .readonly(GACase::getStatus, NEVER_SHOW)
            .done();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        GACase newApp = caseData.getCurrentGeneralApplication();

        CaseLink caseLink = new CaseLink().builder().caseReference(caseReference.toString()).caseType("PCS").build();

        if (newApp != null) {
            GACase gaData = GACase.builder()
                .caseLink(caseLink)
                .gaType(newApp.getGaType())
                .adjustment(newApp.getAdjustment())
                .status(State.DRAFT)
                .additionalInformation(newApp.getAdditionalInformation())
                .build();

            Long gaCaseReference = genAppService.createGeneralApplicationInCCD(
                gaData,
                EventId.createGeneralApplication.name()
            );

            // Retrieve saved entity
            GACaseEntity genApp = genAppService.findByCaseReference(gaCaseReference);
            // Link to parent case
            PcsCaseEntity parentCase = pcsCaseService.findPCSCase(caseReference);
            genApp.setPcsCase(parentCase);
            parentCase.getGeneralApplications().add(genApp);
            pcsCaseService.savePCSCase(parentCase);

            newApp.setCaseReference(gaCaseReference);
            caseData.setCurrentGeneralApplication(null);
        }
    }
}

