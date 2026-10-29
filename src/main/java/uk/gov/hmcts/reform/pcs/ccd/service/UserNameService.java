package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.UserNameRepository;
import uk.gov.hmcts.reform.pcs.idam.IdamUserIds;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

@Service
@AllArgsConstructor
public class UserNameService {

    private final SecurityContextService securityContextService;
    private final UserNameRepository userNameRepository;

    @Transactional
    public UserNameEntity getOrCreateUserNameEntity() {

        UserInfo userInfo = securityContextService.getCurrentUserDetails();
        String idamId = IdamUserIds.normalise(userInfo.getUid());

        return userNameRepository.findByIdamId(idamId)
            .orElseGet(() -> createOrFetchUser(idamId, userInfo.getName()));
    }

    private UserNameEntity createOrFetchUser(String idamId, String name) {
        try {
            return userNameRepository.save(createUserNameEntity(idamId, name));
        } catch (DataIntegrityViolationException e) {
            return userNameRepository.findByIdamId(idamId).orElseThrow(() -> e);
        }
    }

    private UserNameEntity createUserNameEntity(String idamId, String name) {
        return UserNameEntity.builder()
            .idamId(idamId)
            .name(name)
            .build();
    }
}
