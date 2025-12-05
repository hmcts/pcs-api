package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AccessCodeService {

    private final PartyAccessCodeRepository partyAccessCodeRepo;

    public PartyAccessCodeEntity createPartyAccessCodeEntity(PcsCaseEntity  pcsCaseEntity, UUID partyId) {
        String code = AccessCodeGenerator.generateAccessCode();

        PartyAccessCodeEntity partyAccessCodeEntity =  PartyAccessCodeEntity.builder()
            .partyId(partyId)
            .pcsCase(pcsCaseEntity)
            .code(code)
            .role(PartyRole.DEFENDANT)
            .build();
        return partyAccessCodeRepo.save(partyAccessCodeEntity);
    }
}
