package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class DispatchingPartyAccessCodeHashingService implements PartyAccessCodeHashingService {

    private final SimplePartyAccessCodeService cleartextImpl;
    private final HashingPartyAccessCodeService hashedImpl;
    private final FeatureToggleService featureToggle;

    @Override
    public String encodeForStorage(String accessCode) {
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
        return hashedImpl.findMatchingAccessCode(repository, caseId, accessCode);
    }
}
