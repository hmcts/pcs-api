package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.UserNameRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserNameServiceTest {

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private UserNameRepository userNameRepository;

    @InjectMocks
    private UserNameService userNameService;

    @ParameterizedTest
    @ValueSource(strings = {"1245", "idam-user-123", "7FB8BD47-D568-499F-A078-43AFADC74921"})
    void shouldReturnExistingUserIfPresent(String uid) {
        // Given
        String idamId = uid.toLowerCase(Locale.ROOT);
        String idamName = "IDAM Name";
        UserInfo userInfo = UserInfo.builder()
            .uid(uid)
            .name(idamName)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        String userName = "User Name";
        UserNameEntity existingUserNameEntity = UserNameEntity.builder()
            .name(userName)
            .idamId(idamId)
            .build();

        when(userNameRepository.findByIdamId(idamId)).thenReturn(Optional.of(existingUserNameEntity));

        // When
        UserNameEntity userNameEntity = userNameService.getOrCreateUserNameEntity();

        // Then
        assertThat(userNameEntity.getName()).isEqualTo(userName);
        assertThat(userNameEntity.getIdamId()).isEqualTo(idamId);
        verify(userNameRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1245", "idam-user-123", "7FB8BD47-D568-499F-A078-43AFADC74921"})
    void shouldCreateNewUserIfNotPresent(String uid) {
        // Given
        String idamId = uid.toLowerCase(Locale.ROOT);
        String idamName = "IDAM Name";
        UserInfo userInfo = UserInfo.builder()
            .uid(uid)
            .name(idamName)
            .build();

        UserNameEntity savedUser = UserNameEntity.builder()
            .idamId(idamId)
            .name(idamName)
            .build();
        when(userNameRepository.save(any(UserNameEntity.class)))
            .thenReturn(savedUser);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        UserNameEntity userNameEntity = userNameService.getOrCreateUserNameEntity();

        // Then
        assertThat(userNameEntity.getName()).isEqualTo(idamName);
        assertThat(userNameEntity.getIdamId()).isEqualTo(idamId);
        verify(userNameRepository).save(any(UserNameEntity.class));
    }

    @Test
    void shouldHandleUserCreatedByADifferentConcurrentRequest() {
        // Given
        String idamId = "1245";
        String idamName = "IDAM Name";
        UserInfo userInfo = UserInfo.builder()
            .uid(idamId)
            .name(idamName)
            .build();

        UserNameEntity savedUser = UserNameEntity.builder()
            .idamId(idamId)
            .name(idamName)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        doThrow(new DataIntegrityViolationException("duplicate user"))
            .when(userNameRepository)
            .save(any(UserNameEntity.class));

        String userName = "User Name";
        UserNameEntity existingUserNameEntity = UserNameEntity.builder()
            .name(userName)
            .idamId(idamId)
            .build();

        when(userNameRepository.findByIdamId(idamId))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existingUserNameEntity));

        // When
        UserNameEntity userNameEntity = userNameService.getOrCreateUserNameEntity();

        // Then
        assertThat(userNameEntity.getName()).isEqualTo(userName);
        assertThat(userNameEntity.getIdamId()).isEqualTo(idamId);
        verify(userNameRepository).save(any(UserNameEntity.class));
    }

    @Test
    void shouldThrowExceptionIfUserPresentButNotAbleToBeFetched() {
        // Given
        String idamId = "1245";
        String idamName = "IDAM Name";
        UserInfo userInfo = UserInfo.builder()
            .uid(idamId)
            .name(idamName)
            .build();

        UserNameEntity savedUser = UserNameEntity.builder()
            .idamId(idamId)
            .name(idamName)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        doThrow(new DataIntegrityViolationException("duplicate user"))
            .when(userNameRepository)
            .save(any(UserNameEntity.class));

        // When
        assertThrows(DataIntegrityViolationException.class, () -> userNameService.getOrCreateUserNameEntity());
    }
}
