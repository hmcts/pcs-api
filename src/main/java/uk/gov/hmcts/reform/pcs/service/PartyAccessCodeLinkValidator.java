package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;

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

    public void validatePartyNotAlreadyLinked(PartyEntity partyEntity) {
        if (partyEntity.getIdamId() != null) {
            log.error("Access code already linked to user - partyId: {}, existingIdamUserId: {}",
                partyEntity.getId(), partyEntity.getIdamId());
            throw new AccessCodeAlreadyUsedException("This access code is already linked to a user.");
        }
    }

    public void validateUserNotLinkedToAnotherParty(
        List<PartyEntity> partyEntities,
        UUID currentPartyId,
        UUID idamUserId
    ) {
        boolean userIdAlreadyLinked = partyEntities.stream()
            .filter(party -> !party.getId().equals(currentPartyId))
            .anyMatch(party -> idamUserId.equals(party.getIdamId()));

        if (userIdAlreadyLinked) {
            UUID conflictingPartyId = partyEntities.stream()
                .filter(party -> !party.getId().equals(currentPartyId))
                .filter(party -> idamUserId.equals(party.getIdamId()))
                .map(PartyEntity::getId)
                .findFirst()
                .orElse(null);
            log.error(
                "User already linked to different party - attemptedPartyId: {}, idamUserId: {}, linkedToPartyId: {}",
                currentPartyId, idamUserId, conflictingPartyId);
            throw new AccessCodeAlreadyUsedException("This user is already linked to another party in this case.");
        }
    }
}
