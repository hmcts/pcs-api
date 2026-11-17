package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @ParameterizedTest(name = "case with {0} stored codes")
    @ValueSource(ints = {0, 1, 3, 5, HashingPartyAccessCodeService.MATCH_ATTEMPTS})
    void shouldRunFixedNumberOfComparesForWrongCodeWhateverTheNumberOfStoredCodes(int storedCodeCount) {
        PasswordEncoder countingEncoder = spy(new BCryptPasswordEncoder(4));
        PartyAccessCodeHashingService service = new HashingPartyAccessCodeService(countingEncoder);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        List<PartyAccessCodeEntity> storedCodes = storedCodes(countingEncoder, storedCodeCount);
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(storedCodes);

        assertThat(service.findMatchingAccessCode(repository, caseId, "WRONGCODE123")).isEmpty();
        verify(countingEncoder, times(HashingPartyAccessCodeService.MATCH_ATTEMPTS))
            .matches(eq("WRONGCODE123"), anyString());
    }

    @Test
    void shouldRunSameNumberOfComparesForRightCodeAsForWrongCode() {
        PasswordEncoder countingEncoder = spy(new BCryptPasswordEncoder(4));
        PartyAccessCodeHashingService service = new HashingPartyAccessCodeService(countingEncoder);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        PartyAccessCodeEntity firstCode = storedCode(countingEncoder, "CODE00000000");
        List<PartyAccessCodeEntity> storedCodes = List.of(
            firstCode,
            storedCode(countingEncoder, "CODE00000001"),
            storedCode(countingEncoder, "CODE00000002")
        );
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(storedCodes);

        assertThat(service.findMatchingAccessCode(repository, caseId, "CODE00000000")).contains(firstCode);
        verify(countingEncoder, times(HashingPartyAccessCodeService.MATCH_ATTEMPTS))
            .matches(eq("CODE00000000"), anyString());
    }

    @Test
    void shouldCheckEveryStoredCodeWhenCaseHasMoreCodesThanTheCeiling() {
        PasswordEncoder countingEncoder = spy(new BCryptPasswordEncoder(4));
        PartyAccessCodeHashingService service = new HashingPartyAccessCodeService(countingEncoder);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        int storedCodeCount = HashingPartyAccessCodeService.MATCH_ATTEMPTS + 2;
        List<PartyAccessCodeEntity> storedCodes = storedCodes(countingEncoder, storedCodeCount);
        when(repository.findAllByPcsCase_Id(caseId)).thenReturn(storedCodes);

        assertThat(service.findMatchingAccessCode(repository, caseId, "WRONGCODE123")).isEmpty();
        verify(countingEncoder, times(storedCodeCount)).matches(eq("WRONGCODE123"), anyString());
    }

    private static List<PartyAccessCodeEntity> storedCodes(PasswordEncoder encoder, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> storedCode(encoder, "CODE%08d".formatted(i)))
            .toList();
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
