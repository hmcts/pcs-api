package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
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
    private final SystemEventExecutor systemEventExecutor;

    public void linkPartyByAccessCode(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        String idamUserId = IdamUserIds.normalise(userInfo.getUid());

        // An unknown case goes through the same access code check as a known one, against an id with no
        // codes, so the response and the time taken don't reveal whether the case exists. The system event
        // would answer 404 for an unknown case, so this check runs before it.
        PcsCaseEntity caseEntity = findCase(caseReference);
        validator.validateAccessCode(
            caseEntity != null ? caseEntity.getId() : UNKNOWN_CASE_ID,
            accessCode
        );
        if (caseEntity == null) {
            throw new IllegalStateException("Access code matched for an unknown case");
        }

        systemEventExecutor.execute(
            caseReference,
            UUID.randomUUID(),
            context -> linkParty(caseReference, accessCode, idamUserId)
        );
    }

    private SystemEventResult linkParty(long caseReference, String accessCode, String idamUserId) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyAccessCodeEntity pac = validator.validateAccessCode(caseEntity.getId(), accessCode);

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

        return SystemEventResult.withoutStateTransition("defendantLinkedToCase", "Defendant linked to case");
    }

    private PcsCaseEntity findCase(long caseReference) {
        try {
            return pcsCaseService.loadCase(caseReference);
        } catch (CaseNotFoundException e) {
            return null;
        }
    }
}
