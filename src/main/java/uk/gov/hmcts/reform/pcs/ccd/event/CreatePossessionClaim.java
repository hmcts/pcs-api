package uk.gov.hmcts.reform.pcs.ccd.event;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.Address;
import uk.gov.hmcts.reform.pcs.entity.PCSCaseEntity;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;

@Component
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseRepository pcsCaseRepository;
    private final ModelMapper modelMapper;

    public CreatePossessionClaim(PcsCaseRepository pcsCaseRepository,
                                 ModelMapper modelMapper) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.createPossessionClaim.name(), this::submit)
            .initialState(State.Open)
            .name("Create a Property Claim")
            .showSummary()
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .fields()
            .page("property details")
            .mandatory(PCSCase::getPropertyAddress)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        PCSCaseEntity pcsCaseEntity = PCSCaseEntity.builder()
            .caseReference(caseReference)
            .build();

        Address addressEntity = createAddressEntity(pcsCase);
        pcsCaseEntity.setAddress(addressEntity);

        pcsCaseRepository.save(pcsCaseEntity);
    }

    private Address createAddressEntity(PCSCase pcsCase) {
        return modelMapper.map(pcsCase.getPropertyAddress(), Address.class);
    }

}
