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

@Component
public class CreateGeneralApplication implements CCDConfig<GACase, State, UserRole> {

    private final GeneralApplicationService genAppService;

    public CreateGeneralApplication(GeneralApplicationService genAppService) {
        this.genAppService = genAppService;
    }

    @Override
    public void configure(ConfigBuilder<GACase, State, UserRole> builder) {
        builder.decentralisedEvent(EventId.createGeneralApplication.name(), this::submit)
                .initialState(State.DRAFT)
                .name("Make General Application")
                .grant(Permission.CRUD, UserRole.CASE_ADMIN)
                .fields()
                .mandatory(GACase::getGaType)
                .mandatory(GACase::getAdjustment)
                .optional(GACase::getAdditionalInformation)
                .optional(GACase::getCaseLink)
                .optional(GACase::getStatus)
                .optional(GACase::getCaseReference)
                .done();
    }

    private void submit(EventPayload<GACase, State> eventPayload) {
        GACase ga = eventPayload.caseData();
        GACaseEntity entity = genAppService.convertToGAEntity(ga);
        entity.setCaseReference(eventPayload.caseReference());
        entity.setParentCaseReference(Long.valueOf(ga.getCaseLink().getCaseReference()));
        genAppService.saveGaApp(entity);
    }
}
