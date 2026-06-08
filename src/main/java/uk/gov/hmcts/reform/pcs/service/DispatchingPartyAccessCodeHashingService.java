package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Gates how new access codes are stored on the {@code access-code-hashing-enabled} LaunchDarkly
 * flag, while verification stays scheme-agnostic. Only the write path follows the flag; reads always
 * go through the BCrypt impl, which verifies both BCrypt-stored and cleartext-stored codes. That
 * keeps codes verifiable when the flag is flipped either way (no broken rollback). Flipping the flag
 * switches hashing on/off at runtime with no redeploy. Replaces the old {@code @ConditionalOnProperty}.
 */
@Service
@Primary
@RequiredArgsConstructor
public class DispatchingPartyAccessCodeHashingService implements PartyAccessCodeHashingService {

    private final SimplePartyAccessCodeService cleartextImpl;
    private final HashingPartyAccessCodeService hashedImpl;
    private final FeatureToggleService featureToggle;

    @Override
    public String encodeForStorage(String accessCode) {
        // WRITE: the flag decides how new codes are stored.
        return featureToggle.isAccessCodeHashingEnabled()
            ? hashedImpl.encodeForStorage(accessCode)
            : cleartextImpl.encodeForStorage(accessCode);
    }

    @Override
    public Optional<PartyAccessCodeEntity> findMatchingAccessCode(
        PartyAccessCodeRepository repository,
        UUID caseId,
        String accessCode
    ) {
        // READ: never consult the flag. hashedImpl verifies BCrypt-stored (encoder.matches) and
        // cleartext-stored (equality fallback) codes, so verification survives a flag flip either way.
        return hashedImpl.findMatchingAccessCode(repository, caseId, accessCode);
    }
}
