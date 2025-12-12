package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartyAccessCodeLinkValidator {

    private final PartyAccessCodeRepository pacRepository;

    public PartyAccessCodeEntity validateAccessCode(UUID caseId, String accessCode) {
        return pacRepository
            .findByPcsCase_IdAndCode(caseId, accessCode)
            .orElseThrow(() -> new InvalidAccessCodeException("Invalid access code for this case."));
    }

    public Defendant validatePartyBelongsToCase(
        List<Defendant> defendants,
        UUID partyId
    ) {
        return defendants.stream()
            .filter(defendant -> partyId.equals(defendant.getPartyId()))
            .findFirst()
            .orElseThrow(() -> new InvalidPartyForCaseException("Party does not belong to this case."));
    }

    public void validatePartyNotAlreadyLinked(Defendant defendant) {
        if (defendant.getIdamUserId() != null) {
            throw new AccessCodeAlreadyUsedException("This access code is already linked to a user.");
        }
    }

    public void validateUserNotLinkedToAnotherParty(
        List<Defendant> defendants,
        UUID currentPartyId,
        UUID idamUserId
    ) {
        boolean userIdAlreadyLinked = defendants.stream()
            .filter(defendant -> !defendant.getPartyId().equals(currentPartyId))
            .anyMatch(defendant -> idamUserId.equals(defendant.getIdamUserId()));

        if (userIdAlreadyLinked) {
            throw new AccessCodeAlreadyUsedException(
                "This user ID is already linked to another party in this case.");
        }
    }
}
