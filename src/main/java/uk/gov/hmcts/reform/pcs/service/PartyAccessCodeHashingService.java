package uk.gov.hmcts.reform.pcs.service;

import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartyAccessCodeHashingService {

    String encodeForStorage(String accessCode);

    Optional<PartyAccessCodeEntity> findMatchingAccessCode(
        PartyAccessCodeRepository repository,
        UUID caseId,
        String accessCode
    );

    /**
     * Spends the same BCrypt time as a non-matching lookup, for a case that does not exist.
     */
    void matchAgainstDummyCodes(String accessCode);
}
