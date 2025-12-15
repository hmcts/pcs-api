package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyAccessCodeLinkValidatorTest {

    @InjectMocks
    private PartyAccessCodeLinkValidator validator;

    @Mock
    private PartyAccessCodeRepository pacRepository;

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String ACCESS_CODE = "ABCD1234";
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Defendant createDefendant(UUID partyId, UUID idamUserId) {
        Defendant defendant = new Defendant();
        defendant.setPartyId(partyId);
        defendant.setIdamUserId(idamUserId);
        return defendant;
    }

    @Test
    void shouldReturnPac_WhenAccessCodeExists() {
        // GIVEN
        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
            .partyId(PARTY_ID)
            .code(ACCESS_CODE)
            .build();

        when(pacRepository.findByPcsCase_IdAndCode(CASE_ID, ACCESS_CODE))
            .thenReturn(Optional.of(pac));

        // WHEN
        PartyAccessCodeEntity result = validator.validateAccessCode(CASE_ID, ACCESS_CODE);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getPartyId()).isEqualTo(PARTY_ID);
        assertThat(result.getCode()).isEqualTo(ACCESS_CODE);
    }

    @Test
    void shouldThrowInvalidAccessCodeException_WhenAccessCodeNotFound() {
        // GIVEN
        when(pacRepository.findByPcsCase_IdAndCode(CASE_ID, ACCESS_CODE))
            .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validateAccessCode(CASE_ID, ACCESS_CODE))
            .isInstanceOf(InvalidAccessCodeException.class)
            .hasMessageContaining("Invalid data");
    }

    @Test
    void shouldReturnDefendant_WhenPartyBelongsToCase() {
        // GIVEN
        Defendant defendant = createDefendant(PARTY_ID, null);
        List<Defendant> defendants = List.of(defendant);

        // WHEN
        Defendant result = validator.validatePartyBelongsToCase(defendants, PARTY_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getPartyId()).isEqualTo(PARTY_ID);
    }

    @Test
    void shouldThrowInvalidPartyForCaseException_WhenPartyNotInCase() {
        // GIVEN
        UUID differentPartyId = UUID.randomUUID();
        Defendant defendant = createDefendant(differentPartyId, null);
        List<Defendant> defendants = List.of(defendant);

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validatePartyBelongsToCase(defendants, PARTY_ID))
            .isInstanceOf(InvalidPartyForCaseException.class)
            .hasMessageContaining("Invalid data");
    }

    @Test
    void shouldNotThrow_WhenPartyNotLinked() {
        // GIVEN
        Defendant defendant = createDefendant(PARTY_ID, null);

        // WHEN + THEN - Should not throw
        validator.validatePartyNotAlreadyLinked(defendant);
    }

    @Test
    void shouldThrowAccessCodeAlreadyUsedException_WhenPartyAlreadyLinked() {
        // GIVEN
        Defendant defendant = createDefendant(PARTY_ID, USER_ID);

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validatePartyNotAlreadyLinked(defendant))
            .isInstanceOf(AccessCodeAlreadyUsedException.class)
            .hasMessageContaining("already linked to a user");
    }

    @Test
    void shouldNotThrow_WhenUserNotLinkedToAnotherParty() {
        // GIVEN
        UUID partyId1 = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();
        Defendant party1 = createDefendant(partyId1, UUID.randomUUID());
        Defendant party2 = createDefendant(partyId2, null);
        List<Defendant> parties = List.of(party1, party2);

        // WHEN + THEN - Should not throw
        validator.validateUserNotLinkedToAnotherParty(parties, partyId2, USER_ID);
    }

    @Test
    void shouldThrowAccessCodeAlreadyUsedException_WhenUserLinkedToAnotherParty() {
        // GIVEN
        UUID partyId1 = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();
        Defendant party1 = createDefendant(partyId1, USER_ID);
        Defendant party2 = createDefendant(partyId2, null);
        List<Defendant> parties = List.of(party1, party2);

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validateUserNotLinkedToAnotherParty(
            parties, partyId2, USER_ID))
            .isInstanceOf(AccessCodeAlreadyUsedException.class)
            .hasMessageContaining("already linked to another party");
    }
}
