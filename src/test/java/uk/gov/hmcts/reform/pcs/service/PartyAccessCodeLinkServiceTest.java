package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyAccessCodeLinkServiceTest {

    @InjectMocks
    private PartyAccessCodeLinkService service;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PartyAccessCodeLinkValidator validator;

    @Mock
    private CaseAssignmentService caseAssignmentService;

    private static final long CASE_REFERENCE = 123456L;
    private static final String ACCESS_CODE = "ABCD1234";
    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        testUser = createUser();
    }

    private UserInfo createUser() {
        return new UserInfo(null, USER_ID.toString(), null, null, null, List.of());
    }

    private PartyEntity createParty(UUID partyId, UUID idamUserId) {
        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setId(partyId);
        partyEntity.setIdamId(idamUserId);
        return partyEntity;
    }

    private PcsCaseEntity createCaseWithDefendants(UUID caseId, List<PartyEntity> defendantPartyEntities) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(caseId);
        caseEntity.setCaseReference(CASE_REFERENCE);

        ClaimEntity mainClaim = new ClaimEntity();
        defendantPartyEntities.forEach(
            party -> mainClaim.addParty(party, PartyRole.DEFENDANT)
        );

        caseEntity.addClaim(mainClaim);

        return caseEntity;
    }

    @Test
    void shouldLinkSuccessfully_WhenValidCasePacAndDefendant() {
        // GIVEN
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        PartyEntity defendantEntity = createParty(partyId, null);
        PcsCaseEntity caseEntity = createCaseWithDefendants(caseId, List.of(defendantEntity));

        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
            .partyId(partyId)
            .code(ACCESS_CODE)
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(caseAssignmentService.assignDefendantRole(Mockito.anyLong(), Mockito.anyString())).thenReturn(
            mock(CaseAssignmentUserRolesResponse.class));
        when(validator.validateAccessCode(caseId, ACCESS_CODE)).thenReturn(pac);
        when(validator.validatePartyIsADefendant(List.of(defendantEntity), partyId))
            .thenReturn(defendantEntity);
        // validatePartyNotAlreadyLinked and validateUserNotLinkedToAnotherParty are void methods
        doNothing().when(validator).validatePartyNotAlreadyLinked(defendantEntity);
        doNothing().when(validator).validateUserNotLinkedToAnotherParty(
            List.of(defendantEntity), partyId, USER_ID);

        // WHEN
        service.linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, testUser);

        // THEN
        assertThat(defendantEntity.getIdamId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldThrowInvalidAccessCodeException_WhenPacNotFound() {
        // GIVEN
        UUID caseId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(caseId);
        caseEntity.setCaseReference(CASE_REFERENCE);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(validator.validateAccessCode(caseId, ACCESS_CODE))
            .thenThrow(new InvalidAccessCodeException("Invalid access code for this case."));

        // WHEN + THEN
        assertThatThrownBy(() -> service.linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, testUser))
            .isInstanceOf(InvalidAccessCodeException.class)
            .hasMessageContaining("Invalid access code");
    }

    @Test
    void shouldThrowAccessCodeAlreadyUsedException_WhenDefendantAlreadyLinked() {
        // GIVEN
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        PartyEntity defendantEntity = createParty(partyId, UUID.randomUUID());
        PcsCaseEntity caseEntity = createCaseWithDefendants(caseId, List.of(defendantEntity));

        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
            .partyId(partyId)
            .code(ACCESS_CODE)
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(validator.validateAccessCode(caseId, ACCESS_CODE)).thenReturn(pac);
        when(validator.validatePartyIsADefendant(List.of(defendantEntity), partyId))
            .thenReturn(defendantEntity);

        // validatePartyNotAlreadyLinked throws exception (defendant already has idamUserId)
        doThrow(new AccessCodeAlreadyUsedException("This access code is already linked to a user."))
            .when(validator).validatePartyNotAlreadyLinked(defendantEntity);

        // WHEN + THEN
        assertThatThrownBy(() -> service.linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, testUser))
            .isInstanceOf(AccessCodeAlreadyUsedException.class)
            .hasMessageContaining("already linked");
    }

    @Test
    void shouldThrowAccessCodeAlreadyUsedException_WhenUserIdAlreadyLinkedToAnotherDefendant() {
        // GIVEN
        UUID caseId = UUID.randomUUID();
        UUID partyId1 = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();

        // Defendant 1: Already linked to USER_ID
        PartyEntity defendantEntity1 = createParty(partyId1, USER_ID);
        // Defendant 2: Not linked yet (this is the one we're trying to link)
        PartyEntity defendantEntity2 = createParty(partyId2, null);
        List<PartyEntity> allDefendants = List.of(defendantEntity1, defendantEntity2);
        PcsCaseEntity caseEntity = createCaseWithDefendants(caseId, allDefendants);

        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
            .partyId(partyId2) // Access code for defendant 2
            .code(ACCESS_CODE)
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(validator.validateAccessCode(caseId, ACCESS_CODE)).thenReturn(pac);
        when(validator.validatePartyIsADefendant(allDefendants, partyId2))
            .thenReturn(defendantEntity2);
        // validatePartyNotAlreadyLinked passes
        doNothing().when(validator).validatePartyNotAlreadyLinked(defendantEntity2);
        // validateUserNotLinkedToAnotherParty throws exception
        doThrow(new AccessCodeAlreadyUsedException(
            "This user ID is already linked to another party in this case."))
            .when(validator).validateUserNotLinkedToAnotherParty(
                allDefendants, partyId2, USER_ID);

        // WHEN + THEN
        assertThatThrownBy(() -> service.linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, testUser))
            .isInstanceOf(AccessCodeAlreadyUsedException.class)
            .hasMessageContaining("already linked to another");
    }

    @Test
    void shouldLinkSuccessfully_WhenUserIdNotLinkedToAnyDefendant() {
        // GIVEN
        UUID caseId = UUID.randomUUID();
        UUID partyId1 = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();

        // Defendant 1: Linked to different user
        PartyEntity defendantEntity1 = createParty(partyId1, UUID.randomUUID());
        // Defendant 2: Not linked yet
        PartyEntity defendantEntity2 = createParty(partyId2, null);
        List<PartyEntity> allDefendants = List.of(defendantEntity1, defendantEntity2);
        PcsCaseEntity caseEntity = createCaseWithDefendants(caseId, allDefendants);

        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
            .partyId(partyId2)
            .code(ACCESS_CODE)
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(validator.validateAccessCode(caseId, ACCESS_CODE)).thenReturn(pac);
        when(validator.validatePartyIsADefendant(allDefendants, partyId2))
            .thenReturn(defendantEntity2);
        // All validations pass
        doNothing().when(validator).validatePartyNotAlreadyLinked(defendantEntity2);
        doNothing().when(validator).validateUserNotLinkedToAnotherParty(
            allDefendants, partyId2, USER_ID);

        // WHEN
        service.linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, testUser);

        // THEN
        assertThat(defendantEntity2.getIdamId()).isEqualTo(USER_ID);
    }
}
