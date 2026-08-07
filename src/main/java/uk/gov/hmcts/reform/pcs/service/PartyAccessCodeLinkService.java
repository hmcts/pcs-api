package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemCaseEvent;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventActor;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventOutcome;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyAccessCodeLinkService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeLinkValidator validator;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final SystemCaseEventService systemCaseEventService;

    public void linkPartyByAccessCode(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        // Preserve the endpoint's existing not-found response before the system-event service
        // attempts to lock the corresponding CCD case.
        pcsCaseService.loadCase(caseReference);
        systemCaseEventService.submitOnBehalfOf(
            caseReference,
            new SystemCaseEvent("partyLinkedByAccessCode", "Party linked by access code"),
            idempotencyKey(caseReference, userInfo.getUid()),
            actor(userInfo),
            context -> {
                linkParty(caseReference, accessCode, userInfo);
                return SystemCaseEventOutcome.noStateChange();
            }
        );
    }

    private void linkParty(long caseReference, String accessCode, UserInfo userInfo) {
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

        caseRoleAssignmentService.assignRasRole(caseReference, idamUserId.toString(), UserRole.DEFENDANT);
    }

    private SystemCaseEventActor actor(UserInfo userInfo) {
        return new SystemCaseEventActor.IdamUser(
            new uk.gov.hmcts.reform.idam.client.models.UserInfo(
                userInfo.getSub(),
                userInfo.getUid(),
                userInfo.getName(),
                userInfo.getGivenName(),
                userInfo.getFamilyName(),
                userInfo.getRoles()
            )
        );
    }

    private UUID idempotencyKey(long caseReference, String userId) {
        return UUID.nameUUIDFromBytes(
            ("pcs:party-linked-by-access-code:" + caseReference + ":" + userId)
                .getBytes(StandardCharsets.UTF_8)
        );
    }

}
