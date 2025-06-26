package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
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

import java.util.UUID;

@Slf4j
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
        builder.decentralisedEvent("addGeneralApplication", this::aboutToSubmit)
            .forStates(State.Submitted, State.Open)
            .name("Make General Application")
            .showSummary()
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("General Application Details")
            .complex(PCSCase::getCurrentGeneralApplication)
            .mandatory(GeneralApplication::getAdjustment)
            .done()

            .page("Further Information")
            .complex(PCSCase::getCurrentGeneralApplication)
            .optional(GeneralApplication::getAdditionalInformation)
            .done();
    }
    
    private void aboutToSubmit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        GeneralApplication newApp = caseData.getCurrentGeneralApplication();
        //newApp.setApplicationId(UUID.randomUUID().toString());

        if (newApp != null) {
            newApp.setStatus(State.Draft);
        GenApplication genApp = genAppService.convertToGAEntity(newApp);
            genApp.setApplicationId(UUID.randomUUID().toString());

        PCS parentCase = pcsCaseRepository.findByCcdCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("Parent case not found"));
        genApp.setPcsCase(parentCase);
        parentCase.getGeneralApplications().add(genApp);
            pcsCaseRepository.save(parentCase);//cascades and saves the child also
            //genAppRepository.save(genApp);
            newApp.setApplicationId(genApp.getApplicationId());
        caseData.setCurrentGeneralApplication(null);
        }
    }
}

