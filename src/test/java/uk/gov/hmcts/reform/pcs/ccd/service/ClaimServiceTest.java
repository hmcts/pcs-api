package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        when(pcsCase.getClaimAgainstTrespassers()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getClaimDueToRentArrears()).thenReturn(YesOrNo.NO);
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getMediationAttempted()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getMediationAttemptedDetails()).thenReturn("mediation details");
        when(pcsCase.getSettlementAttempted()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getSettlementAttemptedDetails()).thenReturn("settlement details");
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getWantToUploadDocuments()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getApplicationWithClaim()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.YES);

        List<ClaimGroundEntity> expectedClaimGrounds = List.of(mock(ClaimGroundEntity.class));
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(expectedClaimGrounds);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getAgainstTrespassers()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(createdClaimEntity.getClaimCosts()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getPreActionProtocolFollowed()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getMediationAttempted()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getMediationDetails()).isEqualTo("mediation details");
        assertThat(createdClaimEntity.getSettlementAttempted()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getSettlementDetails()).isEqualTo("settlement details");
        assertThat(createdClaimEntity.getAdditionalDefendants()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getAdditionalUnderlesseesOrMortgagees()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getAdditionalDocsProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getGenAppExpected()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);

        Set<ClaimPartyEntity> claimParties = createdClaimEntity.getClaimParties();
        assertThat(claimParties).hasSize(1);
        ClaimPartyEntity claimParty = claimParties.iterator().next();
        assertThat(claimParty.getParty()).isEqualTo(claimantPartyEntity);
        assertThat(claimParty.getRole()).isEqualTo(PartyRole.CLAIMANT);
        assertThat(createdClaimEntity.getClaimGrounds()).containsExactlyElementsOf(expectedClaimGrounds);

        verify(claimRepository).save(createdClaimEntity);
    }

    @Test
    void shouldCreateMainClaim_WithAdditionalReasonsWhenPresent() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("some additional reasons");

        // When
        ClaimEntity createdClaimEntity =
            claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getAdditionalReasons())
            .isEqualTo("some additional reasons");
    }

    @Test
    void shouldCreateMainClaim_WithDefendantCircumstancesDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        VerticalYesNo defendantInfoProvided = VerticalYesNo.YES;
        String circumstancesInfo = "Some circumstance Info";

        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        when(pcsCase.getDefendantCircumstances()).thenReturn(defendantCircumstances);
        when(defendantCircumstances.getDefendantCircumstancesInfo()).thenReturn(circumstancesInfo);
        when(defendantCircumstances.getHasDefendantCircumstancesInfo()).thenReturn(defendantInfoProvided);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getDefendantCircumstances()).isEqualTo(circumstancesInfo);
        assertThat(createdClaimEntity.getDefendantCircumstancesProvided()).isEqualTo(defendantInfoProvided);
    }

    @Test
    void shouldCreateMainClaim_WithClaimantCircumstancesDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        VerticalYesNo claimantInfoProvided = VerticalYesNo.NO;
        String circumstancesInfo = "example circumstance Info";

        ClaimantCircumstances claimantCircumstances = mock(ClaimantCircumstances.class);
        when(pcsCase.getClaimantCircumstances()).thenReturn(claimantCircumstances);
        when(claimantCircumstances.getClaimantCircumstancesSelect()).thenReturn(claimantInfoProvided);
        when(claimantCircumstances.getClaimantCircumstancesDetails()).thenReturn(circumstancesInfo);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getClaimantCircumstances()).isEqualTo(circumstancesInfo);
        assertThat(createdClaimEntity.getClaimantCircumstancesProvided()).isEqualTo(claimantInfoProvided);
    }

    @Test
    void shouldCreateMainClaim_WithoutClaimantTypeDetailsWhenNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        when(pcsCase.getClaimantType()).thenReturn(null);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getClaimantType()).isNull();
    }

    @Test
    void shouldCreateMainClaim_WithoutClaimantTypeDetailsWhenValueCodeIsNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        DynamicStringList claimantTypeList = mock(DynamicStringList.class);
        when(claimantTypeList.getValueCode()).thenReturn(null);
        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getClaimantType()).isNull();
    }

    @ParameterizedTest
    @MethodSource("claimantTypeScenarios")
    void shouldCreateMainClaim_WithClaimantTypeDetails(ClaimantType claimantType) {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                       .code(claimantType.name())
                       .label(claimantType.getLabel())
                       .build())
            .build();

        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getClaimantType()).isEqualTo(claimantType);
    }

    private static Stream<Arguments> claimantTypeScenarios() {
        return Stream.of(
            arguments(ClaimantType.PRIVATE_LANDLORD),
            arguments(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING),
            arguments(ClaimantType.COMMUNITY_LANDLORD),
            arguments(ClaimantType.MORTGAGE_LENDER),
            arguments(ClaimantType.OTHER)
        );
    }

}
