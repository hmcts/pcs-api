package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartyAccessCodeLinkService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeLinkValidator validator;

    @Transactional
    public ValidateAccessCodeResponse linkPartyByAccessCode(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        // 1) Convert user ID
        UUID idamUserId = UUID.fromString(userInfo.getUid());

        // 2) Load case
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        // 3) Validate access code (lookup partyId + caseId)
        PartyAccessCodeEntity pac = validator.validateAccessCode(
            caseEntity.getId(),
            accessCode
        );

        // Note: pac.getRole() indicates party type (DEFENDANT, CLAIMANT, etc.)
        // Current implementation works with defendants, but design is generic

        UUID partyId = pac.getPartyId();

        // 4) Validate party belongs to case
        Defendant party = validator.validatePartyBelongsToCase(
            caseEntity.getDefendants(),
            partyId
        );

        // 5) Validate party not already linked
        validator.validatePartyNotAlreadyLinked(party);

        // 6) Validate user not linked to another party
        validator.validateUserNotLinkedToAnotherParty(
            caseEntity.getDefendants(),
            partyId,
            idamUserId
        );

        // 7) Link user to party (works for any party type)
        party.setIdamUserId(idamUserId);
        caseEntity.setDefendants(caseEntity.getDefendants());
        pcsCaseService.save(caseEntity);

        return new ValidateAccessCodeResponse(caseReference, "linked");
    }

}
