package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        String claimantCircumstancesDetails = UUID.randomUUID().toString();
        String asbDetails = "Some antisocial behaviour details";
        String prohibitedConductDetails = "Some other prohibited conduct details";

        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn(expectedAdditionalReasons);

        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.YES);

        ClaimantCircumstances claimantCircumstances = mock(ClaimantCircumstances.class);
        when(pcsCase.getClaimantCircumstances()).thenReturn(claimantCircumstances);
        when(claimantCircumstances.getClaimantCircumstancesDetails()).thenReturn(claimantCircumstancesDetails);

        when(pcsCase.getApplicationWithClaim()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);
        List<ClaimGroundEntity> expectedClaimGrounds = List.of(mock(ClaimGroundEntity.class));
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(expectedClaimGrounds);

        ASBQuestionsDetailsWales asbQuestionsDetailsWales = mock(ASBQuestionsDetailsWales.class);
        when(pcsCase.getAsbQuestionsWales()).thenReturn(asbQuestionsDetailsWales);
        when(asbQuestionsDetailsWales.getAntisocialBehaviour()).thenReturn(VerticalYesNo.YES);
        when(asbQuestionsDetailsWales.getAntisocialBehaviourDetails()).thenReturn(asbDetails);
        when(asbQuestionsDetailsWales.getIllegalPurposesUse()).thenReturn(VerticalYesNo.NO);
        when(asbQuestionsDetailsWales.getIllegalPurposesUseDetails()).thenReturn(null);
        when(asbQuestionsDetailsWales.getOtherProhibitedConduct()).thenReturn(VerticalYesNo.YES);
        when(asbQuestionsDetailsWales.getOtherProhibitedConductDetails()).thenReturn(prohibitedConductDetails);

        ASBQuestionsWales expectedASBQuestions = ASBQuestionsWales.builder()
            .antisocialBehaviour(true)
            .antisocialBehaviourDetails(asbDetails)
            .illegalPurposesUse(false)
            .illegalPurposesUseDetails(null)
            .otherProhibitedConduct(true)
            .otherProhibitedConductDetails(prohibitedConductDetails)
            .build();

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getSummary()).isEqualTo(expectedClaimName);
        assertThat(createdClaimEntity.getAdditionalReasons()).isEqualTo(expectedAdditionalReasons);
        assertThat(createdClaimEntity.getClaimCosts()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getApplicationWithClaim()).isTrue();
        assertThat(createdClaimEntity.getClaimantCircumstances())
                .isEqualTo(claimantCircumstances.getClaimantCircumstancesDetails());
        assertThat(createdClaimEntity.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);

        Set<ClaimPartyEntity> claimParties = createdClaimEntity.getClaimParties();
        assertThat(claimParties).hasSize(1);

        ClaimPartyEntity claimParty = claimParties.iterator().next();
        assertThat(claimParty.getParty()).isEqualTo(claimantPartyEntity);
        assertThat(claimParty.getRole()).isEqualTo(PartyRole.CLAIMANT);

        assertThat(createdClaimEntity.getClaimGrounds()).containsExactlyElementsOf(expectedClaimGrounds);
        assertThat(createdClaimEntity.getAsbQuestions()).isEqualTo(expectedASBQuestions);

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
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));

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
        SuspensionOfRightToBuyHousingAct expectedSuspensionAct = SuspensionOfRightToBuyHousingAct.SECTION_6A_2;

        SuspensionOfRightToBuy suspension = mock(SuspensionOfRightToBuy.class);
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(suspension);
        when(suspension.getSuspensionOfRightToBuyHousingActs()).thenReturn(expectedSuspensionAct);
        when(suspension.getSuspensionOfRightToBuyReason()).thenReturn(expectedSuspensionReason);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getSuspensionOfRightToBuyHousingAct()).isEqualTo(expectedSuspensionAct);
        assertThat(createdClaimEntity.getSuspensionOfRightToBuyReason()).isEqualTo(expectedSuspensionReason);
    }

    @Test
    void shouldCreateMainClaim_WithDemotionOfTenancyDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedDemotionReason = "some demotion reason";
        String expectedStatementDetails = "some statement details";
        DemotionOfTenancyHousingAct expectedDemotionAct = DemotionOfTenancyHousingAct.SECTION_82A_2;

        DemotionOfTenancy demotion = mock(DemotionOfTenancy.class);
        when(pcsCase.getDemotionOfTenancy()).thenReturn(demotion);
        when(demotion.getDemotionOfTenancyHousingActs()).thenReturn(expectedDemotionAct);
        when(demotion.getDemotionOfTenancyReason()).thenReturn(expectedDemotionReason);
        when(demotion.getStatementOfExpressTermsDetails()).thenReturn(expectedStatementDetails);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getDemotionOfTenancyHousingAct()).isEqualTo(expectedDemotionAct);
        assertThat(createdClaimEntity.getDemotionOfTenancyReason()).isEqualTo(expectedDemotionReason);
        assertThat(createdClaimEntity.getStatementOfExpressTermsDetails()).isEqualTo(expectedStatementDetails);
    }

    @Test
    void shouldCreateMainClaim_WithSuspensionAndDemotionDetails() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedSuspensionReason = "some suspension reason";
        String expectedDemotionReason = "some demotion reason";
        SuspensionOfRightToBuyHousingAct expectedSuspensionAct = SuspensionOfRightToBuyHousingAct.SECTION_6A_2;
        DemotionOfTenancyHousingAct expectedDemotionAct = DemotionOfTenancyHousingAct.SECTION_82A_2;
        String expectedStatementDetails = "some statement details";

        SuspensionOfRightToBuyDemotionOfTenancy combinedOrders = mock(SuspensionOfRightToBuyDemotionOfTenancy.class);
        DemotionOfTenancy demotion = mock(DemotionOfTenancy.class);
        when(pcsCase.getSuspensionOfRightToBuyDemotionOfTenancy()).thenReturn(combinedOrders);
        when(combinedOrders.getSuspensionOfRightToBuyActs()).thenReturn(expectedSuspensionAct);
        when(combinedOrders.getSuspensionOrderReason()).thenReturn(expectedSuspensionReason);
        when(combinedOrders.getDemotionOfTenancyActs()).thenReturn(expectedDemotionAct);
        when(combinedOrders.getDemotionOrderReason()).thenReturn(expectedDemotionReason);
        when(pcsCase.getDemotionOfTenancy()).thenReturn(demotion);
        when(demotion.getStatementOfExpressTermsDetails()).thenReturn(expectedStatementDetails);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");

        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        assertThat(createdClaimEntity.getSuspensionOfRightToBuyHousingAct()).isEqualTo(expectedSuspensionAct);
        assertThat(createdClaimEntity.getSuspensionOfRightToBuyReason()).isEqualTo(expectedSuspensionReason);
        assertThat(createdClaimEntity.getDemotionOfTenancyHousingAct()).isEqualTo(expectedDemotionAct);
        assertThat(createdClaimEntity.getDemotionOfTenancyReason()).isEqualTo(expectedDemotionReason);
        assertThat(createdClaimEntity.getStatementOfExpressTermsDetails()).isEqualTo(expectedStatementDetails);
    }

    @Test
    void shouldCreateMainClaim_WithProhibitedConductWales_WhenProhibitedConductClaimIsYes_WithAllFields() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedWhyMakingClaim = "Some reason for making the claim";
        String expectedDetailsOfTerms = "Some details of terms";

        PeriodicContractTermsWales periodicContractTerms = mock(PeriodicContractTermsWales.class);
        when(pcsCase.getProhibitedConductWalesClaim()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getProhibitedConductWalesWhyMakingClaim()).thenReturn(expectedWhyMakingClaim);
        when(pcsCase.getPeriodicContractTermsWales()).thenReturn(periodicContractTerms);
        when(periodicContractTerms.getAgreedTermsOfPeriodicContract()).thenReturn(VerticalYesNo.YES);
        when(periodicContractTerms.getDetailsOfTerms()).thenReturn(expectedDetailsOfTerms);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getDemotionOfTenancy()).thenReturn(mock(DemotionOfTenancy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        ProhibitedConductWales prohibitedConduct = createdClaimEntity.getProhibitedConduct();
        assertThat(prohibitedConduct).isNotNull();
        assertThat(prohibitedConduct.getClaimForProhibitedConductContract()).isTrue();
        assertThat(prohibitedConduct.getAgreedTermsOfPeriodicContract()).isTrue();
        assertThat(prohibitedConduct.getDetailsOfTerms()).isEqualTo(expectedDetailsOfTerms);
        assertThat(prohibitedConduct.getWhyMakingClaim()).isEqualTo(expectedWhyMakingClaim);
    }

    @Test
    void shouldCreateMainClaim_ProhibitedConductYes_WithoutPeriodicContractTerms() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedWhyMakingClaim = "Some reason for making the claim";

        when(pcsCase.getProhibitedConductWalesClaim()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getProhibitedConductWalesWhyMakingClaim()).thenReturn(expectedWhyMakingClaim);
        when(pcsCase.getPeriodicContractTermsWales()).thenReturn(null);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getDemotionOfTenancy()).thenReturn(mock(DemotionOfTenancy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        ProhibitedConductWales prohibitedConduct = createdClaimEntity.getProhibitedConduct();
        assertThat(prohibitedConduct).isNotNull();
        assertThat(prohibitedConduct.getClaimForProhibitedConductContract()).isTrue();
        assertThat(prohibitedConduct.getAgreedTermsOfPeriodicContract()).isNull();
        assertThat(prohibitedConduct.getDetailsOfTerms()).isNull();
        assertThat(prohibitedConduct.getWhyMakingClaim()).isEqualTo(expectedWhyMakingClaim);
    }

    @Test
    void shouldCreateMainClaim_ProhibitedConductYes_PeriodicContractButAgreedTermsNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedWhyMakingClaim = "Some reason for making the claim";

        PeriodicContractTermsWales periodicContractTerms = mock(PeriodicContractTermsWales.class);
        when(pcsCase.getProhibitedConductWalesClaim()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getProhibitedConductWalesWhyMakingClaim()).thenReturn(expectedWhyMakingClaim);
        when(pcsCase.getPeriodicContractTermsWales()).thenReturn(periodicContractTerms);
        when(periodicContractTerms.getAgreedTermsOfPeriodicContract()).thenReturn(null);
        when(periodicContractTerms.getDetailsOfTerms()).thenReturn("Some details");

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getDemotionOfTenancy()).thenReturn(mock(DemotionOfTenancy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        ProhibitedConductWales prohibitedConduct = createdClaimEntity.getProhibitedConduct();
        assertThat(prohibitedConduct).isNotNull();
        assertThat(prohibitedConduct.getClaimForProhibitedConductContract()).isTrue();
        assertThat(prohibitedConduct.getAgreedTermsOfPeriodicContract()).isNull();
        assertThat(prohibitedConduct.getDetailsOfTerms()).isEqualTo("Some details");
        assertThat(prohibitedConduct.getWhyMakingClaim()).isEqualTo(expectedWhyMakingClaim);
    }

    @Test
    void shouldCreateMainClaim_WithProhibitedConductWales_WhenProhibitedConductClaimIsYes_WithAgreedTermsNo() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        String expectedWhyMakingClaim = "Some reason for making the claim";

        PeriodicContractTermsWales periodicContractTerms = mock(PeriodicContractTermsWales.class);
        when(pcsCase.getProhibitedConductWalesClaim()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getProhibitedConductWalesWhyMakingClaim()).thenReturn(expectedWhyMakingClaim);
        when(pcsCase.getPeriodicContractTermsWales()).thenReturn(periodicContractTerms);
        when(periodicContractTerms.getAgreedTermsOfPeriodicContract()).thenReturn(VerticalYesNo.NO);
        when(periodicContractTerms.getDetailsOfTerms()).thenReturn(null);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getDemotionOfTenancy()).thenReturn(mock(DemotionOfTenancy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        ProhibitedConductWales prohibitedConduct = createdClaimEntity.getProhibitedConduct();
        assertThat(prohibitedConduct).isNotNull();
        assertThat(prohibitedConduct.getClaimForProhibitedConductContract()).isTrue();
        assertThat(prohibitedConduct.getAgreedTermsOfPeriodicContract()).isFalse();
        assertThat(prohibitedConduct.getDetailsOfTerms()).isNull();
        assertThat(prohibitedConduct.getWhyMakingClaim()).isEqualTo(expectedWhyMakingClaim);
    }

    @Test
    void shouldCreateMainClaim_WithProhibitedConductWales_WhenProhibitedConductClaimIsNo() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        when(pcsCase.getProhibitedConductWalesClaim()).thenReturn(VerticalYesNo.NO);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getDemotionOfTenancy()).thenReturn(mock(DemotionOfTenancy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        ProhibitedConductWales prohibitedConduct = createdClaimEntity.getProhibitedConduct();
        assertThat(prohibitedConduct).isNotNull();
        assertThat(prohibitedConduct.getClaimForProhibitedConductContract()).isFalse();
    }

    @Test
    void shouldCreateMainClaim_WithoutProhibitedConductWales_WhenProhibitedConductClaimIsNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PartyEntity claimantPartyEntity = new PartyEntity();

        when(pcsCase.getProhibitedConductWalesClaim()).thenReturn(null);

        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("example reasons");
        when(pcsCase.getClaimingCostsWanted()).thenReturn(VerticalYesNo.NO);
        when(claimGroundService.getGroundsWithReason(pcsCase)).thenReturn(List.of());
        when(pcsCase.getClaimantCircumstances()).thenReturn(mock(ClaimantCircumstances.class));
        when(pcsCase.getSuspensionOfRightToBuy()).thenReturn(mock(SuspensionOfRightToBuy.class));
        when(pcsCase.getDemotionOfTenancy()).thenReturn(mock(DemotionOfTenancy.class));

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);

        // Then
        ProhibitedConductWales prohibitedConduct = createdClaimEntity.getProhibitedConduct();
        assertThat(prohibitedConduct).isNull();
    }

}
