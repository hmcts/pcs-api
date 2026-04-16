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
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimGroundService claimGroundService;
    @Mock
    private PossessionAlternativesService possessionAlternativesService;
    @Mock
    private HousingActWalesService housingActWalesService;
    @Mock
    private AsbProhibitedConductService asbProhibitedConductService;
    @Mock
    private RentArrearsService rentArrearsService;
    @Mock
    private NoticeOfPossessionService noticeOfPossessionService;
    @Mock
    private StatementOfTruthService statementOfTruthService;
    @Mock
    private PCSCase pcsCase;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, claimGroundService, possessionAlternativesService,
                                        housingActWalesService, asbProhibitedConductService, rentArrearsService,
                                        noticeOfPossessionService, statementOfTruthService);
    }

    @Test
    void shouldCreateMainClaim() {
        // Given
        when(pcsCase.getClaimAgainstTrespassers()).thenReturn(SimpleYesNo.YES);
        when(pcsCase.getClaimDueToRentArrears()).thenReturn(YesOrNo.NO);
        when(pcsCase.getClaimingCostsWanted()).thenReturn(SimpleYesNo.YES);
        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(SimpleYesNo.YES);
        when(pcsCase.getMediationAttempted()).thenReturn(SimpleYesNo.NO);
        when(pcsCase.getMediationAttemptedDetails()).thenReturn("mediation details");
        when(pcsCase.getSettlementAttempted()).thenReturn(SimpleYesNo.YES);
        when(pcsCase.getSettlementAttemptedDetails()).thenReturn("settlement details");
        when(pcsCase.getAddAnotherDefendant()).thenReturn(SimpleYesNo.NO);
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(SimpleYesNo.NO);
        when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(SimpleYesNo.YES);
        when(pcsCase.getWantToUploadDocuments()).thenReturn(SimpleYesNo.YES);
        when(pcsCase.getApplicationWithClaim()).thenReturn(SimpleYesNo.NO);
        when(pcsCase.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);

        List<ClaimGroundEntity> expectedClaimGrounds = List.of(mock(ClaimGroundEntity.class));
        when(claimGroundService.createClaimGroundEntities(pcsCase)).thenReturn(expectedClaimGrounds);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getAgainstTrespassers()).isEqualTo(SimpleYesNo.YES);
        assertThat(createdClaimEntity.getDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(createdClaimEntity.getClaimCosts()).isEqualTo(SimpleYesNo.YES);
        assertThat(createdClaimEntity.getPreActionProtocolFollowed()).isEqualTo(SimpleYesNo.YES);
        assertThat(createdClaimEntity.getMediationAttempted()).isEqualTo(SimpleYesNo.NO);
        assertThat(createdClaimEntity.getMediationDetails()).isEqualTo("mediation details");
        assertThat(createdClaimEntity.getSettlementAttempted()).isEqualTo(SimpleYesNo.YES);
        assertThat(createdClaimEntity.getSettlementDetails()).isEqualTo("settlement details");
        assertThat(createdClaimEntity.getAdditionalDefendants()).isEqualTo(SimpleYesNo.NO);
        assertThat(createdClaimEntity.getUnderlesseeOrMortgagee()).isEqualTo(SimpleYesNo.YES);
        assertThat(createdClaimEntity.getAdditionalUnderlesseesOrMortgagees()).isEqualTo(SimpleYesNo.NO);
        assertThat(createdClaimEntity.getAdditionalDocsProvided()).isEqualTo(SimpleYesNo.YES);
        assertThat(createdClaimEntity.getGenAppExpected()).isEqualTo(SimpleYesNo.NO);
        assertThat(createdClaimEntity.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
        assertThat(createdClaimEntity.getClaimGrounds()).containsExactlyElementsOf(expectedClaimGrounds);

        verify(claimRepository).save(createdClaimEntity);
    }

    @Test
    void shouldCreateMainClaim_WithAdditionalReasonsWhenPresent() {
        // Given
        AdditionalReasons additionalReasons = mock(AdditionalReasons.class);
        when(pcsCase.getAdditionalReasonsForPossession()).thenReturn(additionalReasons);
        when(additionalReasons.getReasons()).thenReturn("some additional reasons");

        // When
        ClaimEntity createdClaimEntity =
            claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getAdditionalReasons())
            .isEqualTo("some additional reasons");
    }

    @Test
    void shouldCreateMainClaim_WithDefendantCircumstancesDetails() {
        // Given
        SimpleYesNo defendantInfoProvided = SimpleYesNo.YES;
        String circumstancesInfo = "Some circumstance Info";

        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        when(pcsCase.getDefendantCircumstances()).thenReturn(defendantCircumstances);
        when(defendantCircumstances.getDefendantCircumstancesInfo()).thenReturn(circumstancesInfo);
        when(defendantCircumstances.getHasDefendantCircumstancesInfo()).thenReturn(defendantInfoProvided);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getDefendantCircumstances()).isEqualTo(circumstancesInfo);
        assertThat(createdClaimEntity.getDefendantCircumstancesProvided()).isEqualTo(defendantInfoProvided);
    }

    @Test
    void shouldCreateMainClaim_WithClaimantCircumstancesDetails() {
        // Given
        SimpleYesNo claimantInfoProvided = SimpleYesNo.NO;
        String circumstancesInfo = "example circumstance Info";

        ClaimantCircumstances claimantCircumstances = mock(ClaimantCircumstances.class);
        when(pcsCase.getClaimantCircumstances()).thenReturn(claimantCircumstances);
        when(claimantCircumstances.getClaimantCircumstancesSelect()).thenReturn(claimantInfoProvided);
        when(claimantCircumstances.getClaimantCircumstancesDetails()).thenReturn(circumstancesInfo);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getClaimantCircumstances()).isEqualTo(circumstancesInfo);
        assertThat(createdClaimEntity.getClaimantCircumstancesProvided()).isEqualTo(claimantInfoProvided);
    }

    @Test
    void shouldCreateMainClaim_WithoutClaimantTypeDetailsWhenNull() {
        // Given
        when(pcsCase.getClaimantType()).thenReturn(null);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getClaimantType()).isNull();
    }

    @Test
    void shouldCreateMainClaim_WithoutClaimantTypeDetailsWhenValueCodeIsNull() {
        // Given
        DynamicStringList claimantTypeList = mock(DynamicStringList.class);
        when(claimantTypeList.getValueCode()).thenReturn(null);
        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getClaimantType()).isNull();
    }

    @ParameterizedTest
    @MethodSource("claimantTypeScenarios")
    void shouldCreateMainClaim_WithClaimantTypeDetails(ClaimantType claimantType) {
        // Given
        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                       .code(claimantType.name())
                       .label(claimantType.getLabel())
                       .build())
            .build();

        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getClaimantType()).isEqualTo(claimantType);
    }

    @Test
    void shouldSetAlternativesToPossession() {
        // Given
        PossessionAlternativesEntity possessionAlternativesEntity = mock(PossessionAlternativesEntity.class);
        when(possessionAlternativesService.createPossessionAlternativesEntity(pcsCase))
            .thenReturn(possessionAlternativesEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getPossessionAlternativesEntity()).isEqualTo(possessionAlternativesEntity);
    }

    @Test
    void shouldSetWalesHousingActForWalesProperties() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(WALES);

        HousingActWalesEntity housingActWalesEntity = mock(HousingActWalesEntity.class);
        when(housingActWalesService.createHousingActWalesEntity(pcsCase))
            .thenReturn(housingActWalesEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getHousingActWales()).isEqualTo(housingActWalesEntity);
    }

    @Test
    void shouldNotSetWalesHousingActForNonWalesProperties() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(ENGLAND);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getHousingActWales()).isNull();
        verify(housingActWalesService, never()).createHousingActWalesEntity(pcsCase);
    }

    @Test
    void shouldSetAsbProhibitedConductForWalesProperties() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(WALES);

        AsbProhibitedConductEntity asbProhibitedConductEntity = mock(AsbProhibitedConductEntity.class);
        when(asbProhibitedConductService.createAsbProhibitedConductEntity(pcsCase))
            .thenReturn(asbProhibitedConductEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getAsbProhibitedConductEntity()).isEqualTo(asbProhibitedConductEntity);
    }

    @Test
    void shouldNotSSetAsbProhibitedConductForNonWalesProperties() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(ENGLAND);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getAsbProhibitedConductEntity()).isNull();
        verify(asbProhibitedConductService, never()).createAsbProhibitedConductEntity(pcsCase);
    }

    @Test
    void shouldSetRentArrears() {
        // Given
        RentArrearsEntity rentArrearsEntity = mock(RentArrearsEntity.class);
        when(rentArrearsService.createRentArrearsEntity(pcsCase))
            .thenReturn(rentArrearsEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getRentArrears()).isEqualTo(rentArrearsEntity);
    }

    @Test
    void shouldSetNoticeOfPossession() {
        // Given
        NoticeOfPossessionEntity noticeOfPossessionEntity = mock(NoticeOfPossessionEntity.class);
        when(noticeOfPossessionService.createNoticeOfPossessionEntity(pcsCase))
            .thenReturn(noticeOfPossessionEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getNoticeOfPossession()).isEqualTo(noticeOfPossessionEntity);
    }

    @Test
    void shouldSetStatementOfTruth() {
        // Given
        StatementOfTruthEntity statementOfTruthEntity = mock(StatementOfTruthEntity.class);
        when(statementOfTruthService.createStatementOfTruthEntity(pcsCase))
            .thenReturn(statementOfTruthEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getStatementOfTruth()).isEqualTo(statementOfTruthEntity);
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
