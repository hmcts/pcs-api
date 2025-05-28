package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePossessionGround;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final ModelMapper modelMapper;
    private final PcsCaseRepository pcsCaseRepository;

    @Transactional
    public void createCase(long caseReference, PcsCase pcsCase) {
        AddressUK claimAddress = pcsCase.getClaimAddress();

        AddressEntity addressEntity = claimAddress != null ? modelMapper.map(claimAddress, AddressEntity.class) : null;

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setAddress(addressEntity);
        pcsCaseEntity.setGeneralNotes(pcsCase.getGeneralNotes());

        pcsCase.getGroundsForPossession()
            .stream()
            .map(possessionGround -> {
                CasePossessionGround casePossessionGround = new CasePossessionGround();
                casePossessionGround.setCode(possessionGround);
                return casePossessionGround;
            })
            .forEach(pcsCaseEntity::addPossessionGround);

        pcsCaseRepository.save(pcsCaseEntity);
    }

}
