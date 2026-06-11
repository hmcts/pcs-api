package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

// Write path follows the LD hashing flag; read path is scheme-agnostic so codes stay verifiable
// across a flag flip. Replaces the old @ConditionalOnProperty bean selection.
@Service
@Primary
@RequiredArgsConstructor
public class DispatchingPartyAccessCodeHashingService implements PartyAccessCodeHashingService {

    private final SimplePartyAccessCodeService cleartextImpl;
    private final HashingPartyAccessCodeService hashedImpl;
    private final FeatureToggleService featureToggle;

    @Override
    public String encodeForStorage(String accessCode) {
        // write: the flag picks the storage scheme
        return featureToggle.isEnabled(FeatureFlag.ACCESS_CODE_HASHING)
            ? hashedImpl.encodeForStorage(accessCode)
            : cleartextImpl.encodeForStorage(accessCode);
    }

    @Override
    public Optional<PartyAccessCodeEntity> findMatchingAccessCode(
        PartyAccessCodeRepository repository,
        UUID caseId,
        String accessCode
    ) {
        // read: hashedImpl verifies both schemes, so a flag flip never orphans existing codes
        return hashedImpl.findMatchingAccessCode(repository, caseId, accessCode);
    }
}
