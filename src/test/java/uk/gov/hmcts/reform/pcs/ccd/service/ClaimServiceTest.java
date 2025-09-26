package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimGroundService claimGroundService;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, claimGroundService);
    }

    @Test
    void shouldCreateMainClaim() {
        // Given
        String expectedClaimName = "Main Claim";
        String expectedAdditionalReasons = "some additional reasons";

        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn(expectedAdditionalReasons);

        List<ClaimGroundEntity> expectedClaimGrounds = List.of(mock(ClaimGroundEntity.class));
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(expectedClaimGrounds);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getSummary()).isEqualTo(expectedClaimName);
        assertThat(createdClaimEntity.getAdditionalReasons()).isEqualTo(expectedAdditionalReasons);

        Set<ClaimPartyEntity> claimParties = createdClaimEntity.getClaimParties();
        assertThat(claimParties).hasSize(1);

        ClaimPartyEntity claimParty = claimParties.iterator().next();
        assertThat(claimParty.getParty()).isEqualTo(claimantPartyEntity);
        assertThat(claimParty.getRole()).isEqualTo(PartyRole.CLAIMANT);

        assertThat(createdClaimEntity.getClaimGrounds()).containsExactlyElementsOf(expectedClaimGrounds);

        verify(claimRepository).save(createdClaimEntity);
    }

}

