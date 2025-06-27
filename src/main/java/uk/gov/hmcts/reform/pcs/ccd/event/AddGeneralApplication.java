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
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

@Component
public class AddGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final GeneralApplicationRepository genAppRepository;
    private final GeneralApplicationService genAppService;
    private final PCSCaseService pcsCaseService;
    private final PCSCaseRepository pcsCaseRepository;

    public AddGeneralApplication(GeneralApplicationRepository genAppRepository,
                                 GeneralApplicationService genAppService, PCSCaseService pcsCaseService,
                                 PCSCaseRepository pcsCaseRepository) {
        this.genAppRepository = genAppRepository;
        this.genAppService = genAppService;
        this.pcsCaseService = pcsCaseService;
        this.pcsCaseRepository = pcsCaseRepository;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.addGeneralApplication.name(), this::aboutToSubmit)
            .forStates(State.Submitted, State.Open)
            .name("Make General Application")
            .showSummary()
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("General Application Details")
            .complex(PCSCase::getCurrentGeneralApplication)
            .mandatory(GeneralApplication::getAdjustment)
            .optional(GeneralApplication::getAdditionalInformation)
            .done();
    }

    private void aboutToSubmit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        GeneralApplication newApp = caseData.getCurrentGeneralApplication();
        //newApp.setApplicationId(UUID.randomUUID().toString());

        if (newApp != null) {
            GeneralApplication gaData = GeneralApplication.builder()
                .adjustment(newApp.getAdjustment())
                .additionalInformation(newApp.getAdditionalInformation())
                .build();

//            gaData.put("parentCase", caseReference.toString());
//            gaData.put("status", State.Draft);

            //String userAuthToken = eventPayload.urlParams().getFirst("Authorization");
            String gaCcdReference = genAppService.createGeneralApplicationInCCD(
                gaData,
                EventId.createGeneralApplication.name()
            );

            // Retrieve saved entity
            GenApplication genApp = genAppRepository.findByApplicationId(gaCcdReference)
                .orElseThrow(() -> new IllegalStateException("GA entity not found"));

            // Link to parent case
            PCS parentCase = pcsCaseRepository.findByCcdCaseReference(caseReference)
                .orElseThrow(() -> new IllegalStateException("Parent case not found"));

            genApp.setPcsCase(parentCase);
            parentCase.getGeneralApplications().add(genApp);

            pcsCaseRepository.save(parentCase);//cascades and saves the child also

            newApp.setApplicationId(genApp.getApplicationId());
            caseData.setCurrentGeneralApplication(null);
        }
    }
}

