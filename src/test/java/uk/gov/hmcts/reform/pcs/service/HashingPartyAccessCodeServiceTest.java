package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashingPartyAccessCodeServiceTest {

    private PartyAccessCodeHashingService underTest;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        underTest = new HashingPartyAccessCodeService(encoder);
    }

    @Test
    void shouldHashAndMatch() {
        String accessCode = "ABCD1234";
        String hash = underTest.encodeForStorage(accessCode);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        PartyAccessCodeEntity entity = PartyAccessCodeEntity.builder()
            .partyId(UUID.randomUUID())
            .code(hash)
            .build();
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(List.of(entity));

        assertThat(hash).isNotEqualTo(accessCode);
        assertThat(underTest.findMatchingAccessCode(repository, caseId, accessCode)).isEqualTo(Optional.of(entity));
        assertThat(underTest.findMatchingAccessCode(repository, caseId, "WRONG")).isEmpty();
    }

    @Test
    void shouldThrowWhenAccessCodeBlankOrNull() {
        assertThatThrownBy(() -> underTest.encodeForStorage(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access Code cannot be null or empty");

        assertThatThrownBy(() -> underTest.encodeForStorage(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access Code cannot be null or empty");

        assertThatThrownBy(() -> underTest.encodeForStorage(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access Code cannot be null or empty");
    }
}
