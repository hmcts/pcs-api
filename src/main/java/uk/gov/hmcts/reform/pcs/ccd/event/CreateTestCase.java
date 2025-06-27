package uk.gov.hmcts.reform.pcs.ccd.event;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.Address;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseRepository pcsRepository;
    private final PCSCaseService pcsCaseService;
    private final ModelMapper modelMapper;

    public CreateTestCase(PCSCaseRepository pcsRepository, PCSCaseService pcsCaseService, ModelMapper modelMapper) {
        this.pcsRepository = pcsRepository;
        this.pcsCaseService = pcsCaseService;
        this.modelMapper = modelMapper;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.decentralisedEvent("createTestApplication", this::submit)
            .initialState(State.Open)
            .name("Make a claim")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Make a claim")
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress)
                .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        var pcsEntity = PCS.builder()
            .ccdCaseReference(caseReference)
            .build();
        Address addressEntity = createAddressEntity(pcsCase);
        addressEntity.setPcsCase(pcsEntity);
        pcsEntity.setPropertyAddress(addressEntity);
        pcsRepository.save(pcsEntity);
        pcsCase.setCcdCaseReference(caseReference);
    }

    private Address createAddressEntity(PCSCase pcsCase) {
        return modelMapper.map(pcsCase.getPropertyAddress(), Address.class);
    }
}
