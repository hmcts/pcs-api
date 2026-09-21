package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.UserNameRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserNameServiceTest {

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private UserNameRepository userNameRepository;

    @InjectMocks
    private UserNameService userNameService;

    @Test
    void shouldReturnExistingUserIfPresent() {
        // Given
        UUID idamId = UUID.randomUUID();
        String idamName = "IDAM Name";
        UserInfo userInfo = UserInfo.builder()
            .uid(idamId.toString())
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
    }

    @Test
    void shouldCreateNewUserIfNotPresent() {
        // Given
        UUID idamId = UUID.randomUUID();
        String idamName = "IDAM Name";
        UserInfo userInfo = UserInfo.builder()
            .uid(idamId.toString())
            .name(idamName)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        UserNameEntity userNameEntity = userNameService.getOrCreateUserNameEntity();

        // Then
        assertThat(userNameEntity.getName()).isEqualTo(idamName);
        assertThat(userNameEntity.getIdamId()).isEqualTo(idamId);
    }
}
