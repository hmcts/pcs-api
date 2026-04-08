package uk.gov.hmcts.reform.pcs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PartyAccessCodeHashingService {

    private final PasswordEncoder encoder;
    private final boolean hashPinsEnabled;

    public PartyAccessCodeHashingService(
        PasswordEncoder encoder,
        @Value("${access-code.hash-pins-enabled}") boolean hashPinsEnabled
    ) {
        this.encoder = encoder;
        this.hashPinsEnabled = hashPinsEnabled;
    }


    public String hash(String accessCode) {
        if (accessCode == null || accessCode.isBlank()) {
            throw new IllegalArgumentException("Access Code cannot be null or empty");
        }
        if (!hashPinsEnabled) {
            return accessCode;
        }
        return encoder.encode(accessCode);
    }

    public boolean matches(String accessCode, String storedAccessCode) {
        if (accessCode == null || storedAccessCode == null) {
            return false;
        }
        if (hashPinsEnabled) {
            return encoder.matches(accessCode, storedAccessCode);
        }
        return accessCode.equals(storedAccessCode);
    }

    public boolean isHashPinsEnabled() {
        return hashPinsEnabled;
    }
}
