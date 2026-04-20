package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimplePartyAccessCodeServiceTest {

    private PartyAccessCodeHashingService underTest;

    @BeforeEach
    void setUp() {
        underTest = new SimplePartyAccessCodeService();
    }

    @Test
    void shouldStoreRawCodeAndMatchUsingPlainTextLookup() {
        String accessCode = "ABCD1234";
        String storedCode = underTest.encodeForStorage(accessCode);
        PartyAccessCodeRepository repository = mock(PartyAccessCodeRepository.class);
        UUID caseId = UUID.randomUUID();
        PartyAccessCodeEntity entity = PartyAccessCodeEntity.builder()
            .partyId(UUID.randomUUID())
            .code(storedCode)
            .build();
        when(repository.findByPcsCase_IdAndCode(caseId, accessCode)).thenReturn(Optional.of(entity));

        assertThat(storedCode).isEqualTo(accessCode);
        assertThat(underTest.findMatchingAccessCode(repository, caseId, accessCode)).contains(entity);
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

