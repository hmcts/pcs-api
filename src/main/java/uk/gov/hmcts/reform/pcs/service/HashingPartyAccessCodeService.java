package uk.gov.hmcts.reform.pcs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores access codes BCrypt-encoded. Plaintext never lives in {@code party_access_code.code}.
 *
 * <p>A lookup that finds no match always runs at least {@link #MIN_MATCH_ATTEMPTS} BCrypt compares,
 * padding with a dummy hash, so a wrong code takes as long whether or not the case exists.
 */
@Service
@Slf4j
public class HashingPartyAccessCodeService implements PartyAccessCodeHashingService {

    static final int MIN_MATCH_ATTEMPTS = 2;

    private final PasswordEncoder encoder;
    private final String dummyHash;

    public HashingPartyAccessCodeService(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public String encodeForStorage(String accessCode) {
        if (accessCode == null || accessCode.isBlank()) {
            throw new IllegalArgumentException("Access Code cannot be null or empty");
        }
        return encoder.encode(accessCode);
    }

    @Override
    public Optional<PartyAccessCodeEntity> findMatchingAccessCode(
        PartyAccessCodeRepository repository,
        UUID caseId,
        String accessCode
    ) {
        if (accessCode == null) {
            return Optional.empty();
        }
        int attempts = 0;
        for (PartyAccessCodeEntity entity : repository.findAllByPcsCase_Id(caseId)) {
            if (entity.getCode() == null) {
                continue;
            }
            attempts++;
            if (encoder.matches(accessCode, entity.getCode())) {
                return Optional.of(entity);
            }
        }
        padMatchAttempts(accessCode, attempts);
        return Optional.empty();
    }

    @Override
    public void matchAgainstDummyCodes(String accessCode) {
        if (accessCode != null) {
            padMatchAttempts(accessCode, 0);
        }
    }

    private void padMatchAttempts(String accessCode, int attemptsSoFar) {
        for (int i = attemptsSoFar; i < MIN_MATCH_ATTEMPTS; i++) {
            encoder.matches(accessCode, dummyHash);
        }
    }
}
