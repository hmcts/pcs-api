package uk.gov.hmcts.reform.pcs.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

// Cleartext impl; selected at runtime by DispatchingPartyAccessCodeHashingService when the LD flag is off.
@Service
public class SimplePartyAccessCodeService implements PartyAccessCodeHashingService {

    @Override
    public String encodeForStorage(String accessCode) {
        if (accessCode == null || accessCode.isBlank()) {
            throw new IllegalArgumentException("Access Code cannot be null or empty");
        }
        return accessCode;
    }

    @Override
    public Optional<PartyAccessCodeEntity> findMatchingAccessCode(
        PartyAccessCodeRepository repository,
        UUID caseId,
        String accessCode
    ) {
        return repository.findByPcsCase_IdAndCode(caseId, accessCode);
    }
}
