package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Routes access-code encoding/verification to the cleartext or BCrypt implementation
 * per call, based on the {@code access-code-hashing-enabled} LaunchDarkly flag. Flipping
 * the flag switches hashing on/off at runtime with no redeploy. Replaces the old
 * {@code @ConditionalOnProperty} env-var selection.
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
        return delegate().encodeForStorage(accessCode);
    }

    @Override
    public Optional<PartyAccessCodeEntity> findMatchingAccessCode(
        PartyAccessCodeRepository repository,
        UUID caseId,
        String accessCode
    ) {
        return delegate().findMatchingAccessCode(repository, caseId, accessCode);
    }

    private PartyAccessCodeHashingService delegate() {
        return featureToggle.isAccessCodeHashingEnabled() ? hashedImpl : cleartextImpl;
    }
}
