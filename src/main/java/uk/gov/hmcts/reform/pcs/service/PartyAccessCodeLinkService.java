package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.idam.IdamUserIds;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyAccessCodeLinkService {

    static final UUID UNKNOWN_CASE_ID = new UUID(0L, 0L);

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeLinkValidator validator;
    private final CaseRoleAssignmentService caseRoleAssignmentService;

    @Transactional
    public void linkPartyByAccessCode(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        String idamUserId = IdamUserIds.normalise(userInfo.getUid());

        PcsCaseEntity caseEntity = findCase(caseReference);

        // An unknown case goes through the same access code check as a known one, against an id with no
        // codes, so the response and the time taken don't reveal whether the case exists.
        PartyAccessCodeEntity pac = validator.validateAccessCode(
            caseEntity != null ? caseEntity.getId() : UNKNOWN_CASE_ID,
            accessCode
        );
        if (caseEntity == null) {
            throw new IllegalStateException("Access code matched for an unknown case");
        }

        UUID partyId = pac.getPartyId();

        List<PartyEntity> defendantPartyEntities = caseEntity.getClaims().getFirst()
            .getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .filter(party -> !party.isRemoved())
            .toList();

        PartyEntity partyEntity = validator.validatePartyIsADefendant(defendantPartyEntities,partyId);

        validator.validatePartyNotAlreadyLinked(partyEntity);

        validator.validateUserNotLinkedToAnotherParty(
            defendantPartyEntities,
            partyId,
            idamUserId
        );

        partyEntity.setIdamId(idamUserId);

        caseRoleAssignmentService.assignRasRole(caseReference, idamUserId, UserRole.DEFENDANT);
    }

    private PcsCaseEntity findCase(long caseReference) {
        try {
            return pcsCaseService.loadCase(caseReference);
        } catch (CaseNotFoundException e) {
            return null;
        }
    }
}
