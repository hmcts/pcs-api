package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
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

        caseAssignmentService.assignDefendantRole(caseReference, idamUserId.toString());
    }

}
