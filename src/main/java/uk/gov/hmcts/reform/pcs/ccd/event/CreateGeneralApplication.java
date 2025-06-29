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
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;

@Component
public class CreateGeneralApplication implements CCDConfig<GACase, State, UserRole> {

    private final GeneralApplicationRepository genAppRepository;
    private final GeneralApplicationService genAppService;

    public CreateGeneralApplication(GeneralApplicationRepository genAppRepository, GeneralApplicationService genAppService) {
        this.genAppRepository = genAppRepository;
        this.genAppService = genAppService;
    }


    @Override
    public void configure(ConfigBuilder<GACase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.createGeneralApplication.name(), this::submit)
            .initialState(State.Draft)
            .name("Make General Application")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .mandatory(GACase::getAdjustment)
            .optional(GACase::getAdditionalInformation)
            .done();
    }

    private void submit(EventPayload<GACase, State> eventPayload) {
        GACase ga = eventPayload.caseData();
        if (ga.getStatus() == null) {
            ga.setStatus(State.Draft);
        }
        // Convert to entity and save
        GA entity = genAppService.convertToGAEntity(ga);
        entity.setCaseReference(eventPayload.caseReference()); // application id not being mapped
        genAppRepository.save(entity);

        // Update domain object with generated ID if needed
        ga.setCaseReference(eventPayload.caseReference());

    }
}
