package uk.gov.hmcts.reform.pcs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
        //TODO:For testing if raw code is matched to the stored hash. This will be removed before merging
        log.warn("Access code to be hashed:{}" , accessCode);
        return encoder.encode(accessCode);
    }

    public boolean matches(String accessCode, String storedAccessCode) {
        if (accessCode == null || storedAccessCode == null) {
            return false;
        }
        if (!hashPinsEnabled) {
            return accessCode.equals(storedAccessCode);
        }
        return encoder.matches(accessCode, storedAccessCode);
    }

    public boolean isHashPinsEnabled() {
        return hashPinsEnabled;
    }
}
