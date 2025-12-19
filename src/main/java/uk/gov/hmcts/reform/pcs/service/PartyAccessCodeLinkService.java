package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyAccessCodeLinkService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeLinkValidator validator;
    private final CaseAssignmentService caseAssignmentService;

    @Transactional(rollbackFor = Exception.class)
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

        Defendant party = validator.validatePartyBelongsToCase(
            caseEntity.getDefendants(),
            partyId
        );

        validator.validatePartyNotAlreadyLinked(party);

        validator.validateUserNotLinkedToAnotherParty(
            caseEntity.getDefendants(),
            partyId,
            idamUserId
        );

        party.setIdamUserId(idamUserId);
        caseEntity.setDefendants(caseEntity.getDefendants());
        pcsCaseService.save(caseEntity);

        // Role assignment happens after the main transaction work to ensure
        // that if it fails, it doesn't affect the case save transaction.
        // This try-catch only affects the role assignment, not the main transaction.
        try {
            caseAssignmentService.assignDefendantRole(caseReference, idamUserId.toString());
        } catch (Exception e) {
            // Log error but don't fail the transaction - case assignment is not critical for linking
            // Note: This exception is caught AFTER the main transaction work (save) is complete,
            // so it does not affect the transaction rollback behavior for the case save.
            log.warn("Failed to assign defendant role for case {} and user {}: {}", 
                    caseReference, idamUserId, e.getMessage(), e);
        }
    }

}
