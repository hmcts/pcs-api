package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForAccessCodeException;

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

    private PartyEntity createParty(UUID partyId, UUID idamUserId) {
        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setId(partyId);
        partyEntity.setIdamId(idamUserId);
        return partyEntity;
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
    void shouldReturnParty_WhenPartyIsADefendant() {
        // GIVEN
        PartyEntity partyEntity = createParty(PARTY_ID, null);
        List<PartyEntity> defendantEntities = List.of(partyEntity);

        // WHEN
        PartyEntity result = validator.validatePartyIsADefendant(defendantEntities, PARTY_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PARTY_ID);
    }

    @Test
    void shouldThrowException_WhenPartyIsNotADefendant() {
        // GIVEN
        PartyEntity partyEntity = createParty(PARTY_ID, null);
        List<PartyEntity> defendantEntities = List.of(partyEntity);
        UUID differentPartyId = UUID.randomUUID();

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validatePartyIsADefendant(defendantEntities, differentPartyId))
            .isInstanceOf(InvalidPartyForAccessCodeException.class)
            .hasMessageContaining("The party this access code was generated for is not a defendant in this case");
    }

    @Test
    void shouldNotThrow_WhenPartyNotLinked() {
        // GIVEN
        PartyEntity partyEntity = createParty(PARTY_ID, null);

        // WHEN + THEN - Should not throw
        validator.validatePartyNotAlreadyLinked(partyEntity);
    }

    @Test
    void shouldThrowAccessCodeAlreadyUsedException_WhenPartyAlreadyLinked() {
        // GIVEN
        PartyEntity partyEntity = createParty(PARTY_ID, USER_ID);

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validatePartyNotAlreadyLinked(partyEntity))
            .isInstanceOf(AccessCodeAlreadyUsedException.class)
            .hasMessageContaining("already linked to a user");
    }

    @Test
    void shouldNotThrow_WhenUserNotLinkedToAnotherParty() {
        // GIVEN
        UUID partyId1 = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();
        PartyEntity partyEntity1 = createParty(partyId1, UUID.randomUUID());
        PartyEntity partyEntity2 = createParty(partyId2, null);
        List<PartyEntity> partyEntities = List.of(partyEntity1, partyEntity2);

        // WHEN + THEN - Should not throw
        validator.validateUserNotLinkedToAnotherParty(partyEntities, partyId2, USER_ID);
    }

    @Test
    void shouldThrowAccessCodeAlreadyUsedException_WhenUserLinkedToAnotherParty() {
        // GIVEN
        UUID partyId1 = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();
        PartyEntity partyEntity1 = createParty(partyId1, USER_ID);
        PartyEntity partyEntity2 = createParty(partyId2, null);
        List<PartyEntity> partyEntities = List.of(partyEntity1, partyEntity2);

        // WHEN + THEN
        assertThatThrownBy(() -> validator.validateUserNotLinkedToAnotherParty(
            partyEntities, partyId2, USER_ID))
            .isInstanceOf(AccessCodeAlreadyUsedException.class)
            .hasMessageContaining("already linked to another party");
    }
}
