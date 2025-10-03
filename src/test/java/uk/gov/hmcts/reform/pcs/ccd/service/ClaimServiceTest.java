package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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

        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getApplicationWithClaim()).thenReturn(VerticalYesNo.YES);

        List<ClaimGroundEntity> expectedClaimGrounds = List.of(mock(ClaimGroundEntity.class));
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(expectedClaimGrounds);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getSummary()).isEqualTo(expectedClaimName);
        assertThat(createdClaimEntity.getAdditionalReasons()).isEqualTo(expectedAdditionalReasons);
        assertThat(createdClaimEntity.getCostsClaimed()).isTrue();
        assertThat(createdClaimEntity.getApplicationWithClaim()).isTrue();

        Set<ClaimPartyEntity> claimParties = createdClaimEntity.getClaimParties();
        assertThat(claimParties).hasSize(1);

        ClaimPartyEntity claimParty = claimParties.iterator().next();
        assertThat(claimParty.getParty()).isEqualTo(claimantPartyEntity);
        assertThat(claimParty.getRole()).isEqualTo(PartyRole.CLAIMANT);

        assertThat(createdClaimEntity.getClaimGrounds()).containsExactlyElementsOf(expectedClaimGrounds);

        verify(claimRepository).save(createdClaimEntity);

    }
    @Test
    void shouldCreateMainClaim_WithDefendantCircumstancesDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String circumstancesInfo = "Some circumstance Info";

        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        when(pcsCase.getDefendantCircumstances()).thenReturn(defendantCircumstances);
        when(defendantCircumstances.getDefendantCircumstancesInfo()).thenReturn(circumstancesInfo);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getDefendantCircumstances()).isEqualTo(circumstancesInfo);
    }

    @Test
    void shouldCreateMainClaim_WithSuspensionOfRightToBuyDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedSuspensionReason = "some suspension reason";
        SuspensionOfRightToBuyHousingAct expectedSuspensionAct = SuspensionOfRightToBuyHousingAct.SECTION_62A;

        SuspensionOfRightToBuy suspension = mock(SuspensionOfRightToBuy.class);
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(suspension);
        when(suspension.getSuspensionOfRightToBuyHousingActs()).thenReturn(expectedSuspensionAct);
        when(suspension.getSuspensionOfRightToBuyReason()).thenReturn(expectedSuspensionReason);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getSuspensionOfRightToBuyHousingAct()).isEqualTo(expectedSuspensionAct);
        assertThat(createdClaimEntity.getSuspensionOfRightToBuyReason()).isEqualTo(expectedSuspensionReason);
    }
}

