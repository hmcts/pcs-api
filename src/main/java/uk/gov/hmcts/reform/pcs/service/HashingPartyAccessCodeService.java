package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "access-code.hash-pins-enabled", havingValue = "true")
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
            .filter(entity -> encoder.matches(accessCode, entity.getCode()))
            .findFirst();
    }
}

