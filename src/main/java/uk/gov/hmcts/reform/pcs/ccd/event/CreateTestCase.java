package uk.gov.hmcts.reform.pcs.ccd.event;


import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimantInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCSCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestApplication;


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
        configBuilder
            .decentralisedEvent(createTestApplication.name(), this::submit)
            .initialState(State.CASE_ISSUED)
            .name("Make a claim")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
            .fields()
            .page("claimant information")
            .pageLabel("Please enter applicant's name")
            .mandatory(PCSCase::getApplicantForename)
            .mandatory(PCSCase::getApplicantSurname)
            .page("Make a claim")
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress)
            .page("claimant information")
            .done();

    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        var pcsEntity = PCSCaseEntity.builder()
            .caseReference(caseReference)
            .build();
        AddressEntity addressEntity = createAddressEntity(pcsCase);
        addressEntity.setPcsCase(pcsEntity);
        ClaimantInfo claimantInfoEntity = createClaimantInfoEntity(pcsCase);
        claimantInfoEntity.setPcsCase(pcsEntity);

        pcsEntity.setPropertyAddress(addressEntity);
        pcsEntity.setClaimantInfo(claimantInfoEntity);

        PCSCaseEntity savedEntity = pcsRepository.save(pcsEntity);
        pcsCase.setCaseReference(savedEntity.getCaseReference());
    }

    private AddressEntity createAddressEntity(PCSCase pcsCase) {
        return modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
    }

    private ClaimantInfo createClaimantInfoEntity(PCSCase pcsCase) {
        ClaimantInfo claimantInfo = ClaimantInfo.builder().forename(pcsCase.getApplicantForename())
            .surname(pcsCase.getApplicantSurname()).build();
        return claimantInfo;
    }

}


