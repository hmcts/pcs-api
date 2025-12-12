package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeGenerationService {

    private final PartyAccessCodeRepository partyAccessCodeRepo;
    private final PcsCaseService pcsCaseService;
    private final AccessCodeGenerator accessCodeGenerator;

    public PartyAccessCodeEntity createPartyAccessCodeEntity(PcsCaseEntity  pcsCaseEntity, UUID partyId) {
        String code = accessCodeGenerator.generateAccessCode();

        return PartyAccessCodeEntity.builder()
            .partyId(partyId)
            .pcsCase(pcsCaseEntity)
            .code(code)
            .role(PartyRole.DEFENDANT)
            .build();
    }

    @Transactional
    public void createAccessCodesForParties(String caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(Long.valueOf(caseReference));

        Set<UUID> existingPartyIds = partyAccessCodeRepo.findAllByPcsCase_Id(pcsCaseEntity.getId())
            .stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .collect(Collectors.toSet());

        Set<PartyAccessCodeEntity> newCodes = pcsCaseEntity.getDefendants().stream()
            .filter(d -> !existingPartyIds.contains(d.getPartyId()))
            .map(d -> createPartyAccessCodeEntity(pcsCaseEntity, d.getPartyId()))
            .collect(Collectors.toSet());

        if (!newCodes.isEmpty()) {
            partyAccessCodeRepo.saveAll(newCodes);
            log.debug("Created {} new access codes for case {}", newCodes.size(), caseReference);
        }
    }
}
