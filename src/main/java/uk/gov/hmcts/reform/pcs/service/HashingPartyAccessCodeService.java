package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

// BCrypt impl, used when the hashing flag is on.
@Service
@RequiredArgsConstructor
@Slf4j
public class HashingPartyAccessCodeService implements PartyAccessCodeHashingService {

    private final PasswordEncoder encoder;

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
        return repository.findAllByPcsCase_Id(caseId).stream()
            .filter(entity -> entity.getCode() != null)
            .filter(entity -> matches(accessCode, entity.getCode()))
            .findFirst();
    }

    // Codes minted while the flag was off are stored cleartext; pick the comparator by the stored
    // value's shape (BCrypt hashes start with "$2") so a cleartext value never hits encoder.matches
    // (which would log a "does not look like BCrypt" warning) and still verifies by equality.
    private boolean matches(String accessCode, String storedCode) {
        return storedCode.startsWith("$2")
            ? encoder.matches(accessCode, storedCode)
            : accessCode.equals(storedCode);
    }
}

