package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.AccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.AccessCodeRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AccessCodeService {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPRSTVWXYZ23456789";

    private final AccessCodeRepository accessCodeRepository;

    public String createAccessCode(long caseReference, UserRole role) {
        String accessCode = generateAccessCode();

        AccessCodeEntity accessCodeEntity = AccessCodeEntity.builder()
            .caseReference(caseReference)
            .code(accessCode)
            .role(role)
            .created(Instant.now())
            .build();

        accessCodeRepository.save(accessCodeEntity);

        return accessCode;
    }

    public Optional<AccessCodeEntity> findAccessCode(long caseReference, String code) {
        return accessCodeRepository.findByCaseReferenceAndCode(caseReference, code);
    }

    public void deleteAccessCode(AccessCodeEntity accessCodeEntity) {
        accessCodeRepository.delete(accessCodeEntity);
    }

    private String generateAccessCode() {
        return RandomStringUtils.random(12, 0, ALLOWED_CHARS.length(),
                                        false, false, ALLOWED_CHARS.toCharArray(), new SecureRandom());
    }

}
