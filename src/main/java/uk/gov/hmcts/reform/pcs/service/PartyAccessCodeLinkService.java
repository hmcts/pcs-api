package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyAccessCodeLinkService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeLinkValidator validator;
    private final CaseAssignmentService caseAssignmentService;

    @Transactional
    public void linkPartyByAccessCode(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        UUID idamUserId = UUID.fromString(userInfo.getUid());

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyAccessCodeEntity pac = validator.validateAccessCode(
            caseEntity.getId(),
            accessCode
        );

        UUID partyId = pac.getPartyId();

        List<PartyEntity> defendantPartyEntities = caseEntity.getClaims().getFirst()
            .getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();

        PartyEntity partyEntity = validator.validatePartyIsADefendant(defendantPartyEntities,partyId);

        validator.validatePartyNotAlreadyLinked(partyEntity);

        validator.validateUserNotLinkedToAnotherParty(
            defendantPartyEntities,
            partyId,
            idamUserId
        );

        partyEntity.setIdamId(idamUserId);

        caseAssignmentService.assignDefendantRole(caseReference, idamUserId.toString());
    }

}
