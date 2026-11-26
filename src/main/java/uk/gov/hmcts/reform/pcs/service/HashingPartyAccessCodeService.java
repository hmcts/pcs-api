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
 * <p>Every lookup runs at least {@link #MATCH_ATTEMPTS} BCrypt compares: it checks every stored code
 * without stopping at a match, then pads with a dummy hash. The time taken therefore does not depend on
 * whether the code is right, how many codes the case has, or whether the case exists at all.
 */
@Service
@Slf4j
public class HashingPartyAccessCodeService implements PartyAccessCodeHashingService {

    /**
     * Above any realistic number of defendants on a case, so every lookup costs the same.
     */
    static final int MATCH_ATTEMPTS = 10;

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
        PartyAccessCodeEntity match = null;
        int attempts = 0;
        for (PartyAccessCodeEntity entity : repository.findAllByPcsCase_Id(caseId)) {
            if (entity.getCode() == null) {
                continue;
            }
            attempts++;
            if (encoder.matches(accessCode, entity.getCode()) && match == null) {
                match = entity;
            }
        }
        for (int i = attempts; i < MATCH_ATTEMPTS; i++) {
            encoder.matches(accessCode, dummyHash);
        }
        return Optional.ofNullable(match);
    }
}
