package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantAccessValidatorTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @InjectMocks
    private DefendantAccessValidator underTest;

    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;

    @Test
    void shouldReturnDefendantWhenUserHasAccess() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity caseEntity = createCaseWithClaim(claimEntity);
        List<PartyEntity> defendants = List.of(defendantEntity);
        when(defendantPartyExtractor.extractDefendants(caseEntity, CASE_REFERENCE)).thenReturn(defendants);

        // When
        PartyEntity result = underTest.validateAndGetDefendant(caseEntity, defendantUserId);

        // Then
        assertThat(result).isEqualTo(defendantEntity);
        assertThat(result.getIdamId()).isEqualTo(defendantUserId);
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity caseEntity = createCaseWithClaim(claimEntity);

        // When / Then
        assertThatThrownBy(() -> underTest.validateAndGetDefendant(caseEntity, differentUserId))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    private ClaimEntity createClaimWithDefendant(PartyEntity defendant) {
        ClaimEntity claimEntity = ClaimEntity.builder().build();
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .party(defendant)
            .role(PartyRole.DEFENDANT)
            .build();
        claimEntity.getClaimParties().add(claimPartyEntity);
        return claimEntity;
    }

    private PcsCaseEntity createCaseWithClaim(ClaimEntity claim) {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .build();
        caseEntity.getClaims().add(claim);
        return caseEntity;
    }
}
