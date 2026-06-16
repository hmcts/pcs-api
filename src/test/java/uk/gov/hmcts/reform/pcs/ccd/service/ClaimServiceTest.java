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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, 8, 27)
        .atTime(12, 51, 19);

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimGroundService claimGroundService;
    @Mock
    private PossessionAlternativesService possessionAlternativesService;
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
    @Mock
    private Clock utcClock;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, claimGroundService, possessionAlternativesService,
                                        asbProhibitedConductService, rentArrearsService,
                                        noticeOfPossessionService, statementOfTruthService, utcClock);
        lenient().when(claimRepository.save(any(ClaimEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateMainClaim() {
        // Given
        when(pcsCase.getClaimAgainstTrespassers()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getClaimDueToRentArrears()).thenReturn(YesOrNo.NO);
        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getMediationAttempted()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getSettlementAttempted()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getWantToUploadDocuments()).thenReturn(VerticalYesNo.YES);
        when(pcsCase.getApplicationWithClaim()).thenReturn(VerticalYesNo.NO);
        when(pcsCase.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);
        when(pcsCase.getPreActionProtocolIncompleteExplanation()).thenReturn("explanation");

        List<ClaimGroundEntity> expectedClaimGrounds = List.of(mock(ClaimGroundEntity.class));
        when(claimGroundService.createClaimGroundEntities(pcsCase)).thenReturn(expectedClaimGrounds);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getAgainstTrespassers()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(createdClaimEntity.getPreActionProtocolFollowed()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getMediationAttempted()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getSettlementAttempted()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getAdditionalDefendants()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getAdditionalUnderlesseesOrMortgagees()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getAdditionalDocsProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getGenAppExpected()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
        assertThat(createdClaimEntity.getClaimGrounds()).containsExactlyElementsOf(expectedClaimGrounds);
        assertThat(createdClaimEntity.getPreActionProtocolIncompleteExplanation()).isEqualTo("explanation");

        verify(claimRepository).save(createdClaimEntity);
    }

    @Test
    void shouldReturnSavedClaimEntity() {
        // Given
        ClaimEntity savedClaimEntity = mock(ClaimEntity.class);
        when(claimRepository.save(any(ClaimEntity.class))).thenReturn(savedClaimEntity);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity).isSameAs(savedClaimEntity);
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
        VerticalYesNo defendantInfoProvided = VerticalYesNo.YES;
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
        VerticalYesNo claimantInfoProvided = VerticalYesNo.NO;
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
    void shouldNotSetAsbProhibitedConductForNonWalesProperties() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(ENGLAND);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getAsbProhibitedConductEntity()).isNull();
        verify(asbProhibitedConductService, never()).createAsbProhibitedConductEntity(pcsCase);
    }

    @Test
    void shouldNotSetWalesDocumentsForNonWalesProperties() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(ENGLAND);

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getEnergyPerformanceCertificateProvided()).isNull();
        assertThat(createdClaimEntity.getGasSafetyReportProvided()).isNull();
        assertThat(createdClaimEntity.getElectricalInstallationConditionProvided()).isNull();
        assertThat(createdClaimEntity.getNoEnergyPerformanceCertificateReason()).isNull();
        assertThat(createdClaimEntity.getNoGasSafetyReportReason()).isNull();
        assertThat(createdClaimEntity.getNoElectricalInstallationConditionReason()).isNull();
        verify(pcsCase, never()).getRequiredDocumentsWales();
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

    @Test
    void shouldSetWalesDocuments() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(WALES);
        when(pcsCase.getRequiredDocumentsWales()).thenReturn(
            WalesDocuments.builder()
                .hasEnergyPerformanceCertificate(VerticalYesNo.NO)
                .hasGasSafetyReport(VerticalYesNo.NO)
                .hasElectricalInstallationConditionReport(VerticalYesNo.NO)
                .noEpcReason("noEpcReason")
                .noGasReportReason("noGasReportReason")
                .noEicrReason("noEicrReason")
                .build()
        );

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getEnergyPerformanceCertificateProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getGasSafetyReportProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getElectricalInstallationConditionProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(createdClaimEntity.getNoEnergyPerformanceCertificateReason())
            .isEqualTo("noEpcReason");
        assertThat(createdClaimEntity.getNoGasSafetyReportReason()).isEqualTo("noGasReportReason");
        assertThat(createdClaimEntity.getNoElectricalInstallationConditionReason())
            .isEqualTo("noEicrReason");
    }

    @Test
    void shouldSetNotWalesDocumentsReasonsIfUserHasDocuments() {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(WALES);
        when(pcsCase.getRequiredDocumentsWales()).thenReturn(
            WalesDocuments.builder()
                .hasEnergyPerformanceCertificate(VerticalYesNo.YES)
                .hasGasSafetyReport(VerticalYesNo.YES)
                .hasElectricalInstallationConditionReport(VerticalYesNo.YES)
                .noEpcReason("noEpcReason")
                .noGasReportReason("noGasReportReason")
                .noEicrReason("noEicrReason")
                .build()
        );

        // When
        ClaimEntity createdClaimEntity = claimService.createMainClaimEntity(pcsCase);

        // Then
        assertThat(createdClaimEntity.getEnergyPerformanceCertificateProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getGasSafetyReportProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getElectricalInstallationConditionProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(createdClaimEntity.getNoEnergyPerformanceCertificateReason()).isNull();
        assertThat(createdClaimEntity.getNoGasSafetyReportReason()).isNull();
        assertThat(createdClaimEntity.getNoElectricalInstallationConditionReason()).isNull();
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
