package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.util.List;
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
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(Long.parseLong(caseReference));

        if (CollectionUtils.isEmpty(pcsCaseEntity.getClaims())) {
            log.warn("Skipping access code generation for case {} - no claims found", caseReference);
            return;
        }

        Set<UUID> existingPartyIds = partyAccessCodeRepo.findAllByPcsCase_Id(pcsCaseEntity.getId())
            .stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .collect(Collectors.toSet());

        List<PartyEntity> defendants = getMainClaimDefendants(pcsCaseEntity);

        Set<PartyAccessCodeEntity> newCodes = defendants.stream()
            .filter(d -> !existingPartyIds.contains(d.getId()))
            .map(d -> createPartyAccessCodeEntity(pcsCaseEntity, d.getId()))
            .collect(Collectors.toSet());

        if (!newCodes.isEmpty()) {
            partyAccessCodeRepo.saveAll(newCodes);
            log.debug("Created {} new access codes for case {}", newCodes.size(), caseReference);
        }
    }

    private static List<PartyEntity> getMainClaimDefendants(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity mainClaim = getMainClaim(pcsCaseEntity);

        return mainClaim.getClaimParties()
            .stream()
            .filter(claimPartyEntity -> PartyRole.DEFENDANT == claimPartyEntity.getRole())
            .map(ClaimPartyEntity::getParty)
            .toList();
    }

    // TODO: Will be refactored as part of HDPI-3232
    private static ClaimEntity getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims()
            .stream()
            .findFirst()
            .orElseThrow(() -> new ClaimNotFoundException(pcsCaseEntity.getCaseReference()));
    }
}
