package uk.gov.hmcts.reform.pcs.ccd.event;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.Address;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;

@Component
public class CreatePossessionClaim implements CCDConfig<PcsCase, State, UserRole> {

    private final PcsCaseRepository pcsCaseRepository;
    private final ModelMapper modelMapper;

    public CreatePossessionClaim(PcsCaseRepository pcsCaseRepository,
                                 ModelMapper modelMapper) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.createPossessionClaim.name(), this::submit)
            .initialState(State.Open)
            .name("Create possession claim")
            .showSummary()
            .grant(Permission.CR, UserRole.CREATOR_NO_READ, UserRole.CREATOR_WITH_UPDATE)
            .fields()
            .page("property details")
            .mandatory(PcsCase::getPropertyAddress)
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PcsCase pcsCase = eventPayload.caseData();

        uk.gov.hmcts.reform.pcs.entity.PcsCase pcsCaseEntity = uk.gov.hmcts.reform.pcs.entity.PcsCase.builder()
            .caseReference(caseReference)
            .build();

        Address addressEntity = createAddressEntity(pcsCase);
        pcsCaseEntity.setAddress(addressEntity);

        pcsCaseRepository.save(pcsCaseEntity);
    }

    private Address createAddressEntity(PcsCase pcsCase) {
        return modelMapper.map(pcsCase.getPropertyAddress(), Address.class);
    }

}
