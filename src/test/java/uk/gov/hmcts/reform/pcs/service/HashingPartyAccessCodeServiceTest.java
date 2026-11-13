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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    void shouldRunSameNumberOfComparesForUnknownCaseAsForWrongCode() {
        PasswordEncoder countingEncoder = spy(new BCryptPasswordEncoder(4));
        PartyAccessCodeHashingService service = new HashingPartyAccessCodeService(countingEncoder);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        List<PartyAccessCodeEntity> storedCodes = List.of(storedCode(countingEncoder, "ABCD1234"));
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(storedCodes);

        service.matchAgainstDummyCodes("WRONGCODE123");
        verify(countingEncoder, times(2)).matches(eq("WRONGCODE123"), anyString());

        clearInvocations(countingEncoder);
        assertThat(service.findMatchingAccessCode(repository, caseId, "WRONGCODE123")).isEmpty();
        verify(countingEncoder, times(2)).matches(eq("WRONGCODE123"), anyString());
    }

    @Test
    void shouldPadToMinimumComparesWhenCaseHasNoCodes() {
        PasswordEncoder countingEncoder = spy(new BCryptPasswordEncoder(4));
        PartyAccessCodeHashingService service = new HashingPartyAccessCodeService(countingEncoder);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(List.of());

        assertThat(service.findMatchingAccessCode(repository, caseId, "WRONGCODE123")).isEmpty();
        verify(countingEncoder, times(2)).matches(eq("WRONGCODE123"), anyString());
    }

    @Test
    void shouldNotPadWhenCaseHasMoreCodesThanMinimum() {
        PasswordEncoder countingEncoder = spy(new BCryptPasswordEncoder(4));
        PartyAccessCodeHashingService service = new HashingPartyAccessCodeService(countingEncoder);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        List<PartyAccessCodeEntity> storedCodes = List.of(
            storedCode(countingEncoder, "AAAA1111"),
            storedCode(countingEncoder, "BBBB2222"),
            storedCode(countingEncoder, "CCCC3333")
        );
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(storedCodes);

        assertThat(service.findMatchingAccessCode(repository, caseId, "WRONGCODE123")).isEmpty();
        verify(countingEncoder, times(3)).matches(eq("WRONGCODE123"), anyString());
    }

    private static PartyAccessCodeEntity storedCode(PasswordEncoder encoder, String accessCode) {
        return PartyAccessCodeEntity.builder()
            .partyId(UUID.randomUUID())
            .code(encoder.encode(accessCode))
            .build();
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
