package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeService {

    private final PartyAccessCodeRepository partyAccessCodeRepo;
    private final PcsCaseRepository pcsCaseRepo;

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


    public void createAccessCodesForDefendants(String caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepo
            .findByCaseReference(Long.valueOf(caseReference))
            .orElseThrow(() -> new CaseNotFoundException(Long.valueOf(caseReference)));

        pcsCaseEntity.getDefendants().forEach(defendant -> {
            boolean exists = partyAccessCodeRepo.findByPcsCase_IdAndPartyId(pcsCaseEntity.getId(),
                                                                       defendant.getPartyId()).isPresent();

            if (!exists) {
                createPartyAccessCodeEntity(pcsCaseEntity, defendant.getPartyId());
                log.debug("Access code created for defendant {}", defendant.getPartyId());
            } else {
                log.debug("Access code already exists for defendant {}", defendant.getPartyId());
            }
        });
    }
}
