package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyAccessCodeLinkValidator {

    private final PartyAccessCodeRepository pacRepository;

    public PartyAccessCodeEntity validateAccessCode(UUID caseId, String accessCode) {
        return pacRepository
            .findByPcsCase_IdAndCode(caseId, accessCode)
            .orElseThrow(() -> {
                log.error("Invalid access code - caseId: {}, accessCodeLength: {}, accessCodeProvided: {}",
                    caseId, accessCode != null ? accessCode.length() : 0, accessCode != null);
                return new InvalidAccessCodeException("Invalid data");
            });
    }

    public Defendant validatePartyBelongsToCase(
        List<Defendant> defendants,
        UUID partyId
    ) {
        return defendants.stream()
            .filter(defendant -> partyId.equals(defendant.getPartyId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error(
                    "Party does not belong to case - partyId: {}, totalDefendants: {}, availablePartyIds: {}",
                    partyId, defendants.size(), defendants.stream().map(Defendant::getPartyId).toList());
                return new InvalidPartyForCaseException("Invalid data");
            });
    }

    public void validatePartyNotAlreadyLinked(Defendant defendant) {
        if (defendant.getIdamUserId() != null) {
            log.error("Access code already linked to user - partyId: {}, existingIdamUserId: {}",
                defendant.getPartyId(), defendant.getIdamUserId());
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
            UUID conflictingPartyId = defendants.stream()
                .filter(defendant -> !defendant.getPartyId().equals(currentPartyId))
                .filter(defendant -> idamUserId.equals(defendant.getIdamUserId()))
                .map(Defendant::getPartyId)
                .findFirst()
                .orElse(null);
            log.error(
                "User already linked to different party - attemptedPartyId: {}, idamUserId: {}, linkedToPartyId: {}",
                currentPartyId, idamUserId, conflictingPartyId);
            throw new AccessCodeAlreadyUsedException("This user is already linked to another party in this case.");
        }
    }
}
