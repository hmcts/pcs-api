package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimViewTest {

    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private ClaimEntity claimEntity;

    private ClaimView underTest;

    @BeforeEach
    void setUp() {
        pcsCase = PCSCase.builder().build();

        underTest = new ClaimView();
    }

    @Test
    void shouldMapBasicClaimFields() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getAgainstTrespassers()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getDueToRentArrears()).thenReturn(YesOrNo.NO);
        when(claimEntity.getClaimCosts()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getPreActionProtocolFollowed()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getMediationAttempted()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getMediationDetails()).thenReturn("mediation details");
        when(claimEntity.getSettlementAttempted()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getSettlementDetails()).thenReturn("settlement details");
        when(claimEntity.getAdditionalDefendants()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getAdditionalUnderlesseesOrMortgagees()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getGenAppExpected()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);
        when(claimEntity.getAdditionalDocsProvided()).thenReturn(VerticalYesNo.YES);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getClaimAgainstTrespassers()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getClaimDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getClaimingCostsWanted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getMediationAttempted()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getMediationAttemptedDetails()).isEqualTo("mediation details");
        assertThat(pcsCase.getSettlementAttempted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getSettlementAttemptedDetails()).isEqualTo("settlement details");
        assertThat(pcsCase.getAddAnotherDefendant()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getHasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getApplicationWithClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
        assertThat(pcsCase.getWantToUploadDocuments()).isEqualTo(VerticalYesNo.YES);
    }

    @ParameterizedTest
    @MethodSource("complexClaimFieldsScenarios")
    void shouldMapComplexClaimFields(
        VerticalYesNo claimantSelect,
        String claimantDetails,
        VerticalYesNo defendantSelect,
        String defendantDetails,
        VerticalYesNo additionalReasonsProvided,
        String additionalReasons,
        ClaimantType claimantType
    ) {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getClaimantCircumstancesProvided()).thenReturn(claimantSelect);
        when(claimEntity.getClaimantCircumstances()).thenReturn(claimantDetails);

        when(claimEntity.getDefendantCircumstancesProvided()).thenReturn(defendantSelect);
        when(claimEntity.getDefendantCircumstances()).thenReturn(defendantDetails);

        when(claimEntity.getAdditionalReasonsProvided()).thenReturn(additionalReasonsProvided);
        when(claimEntity.getAdditionalReasons()).thenReturn(additionalReasons);

        when(claimEntity.getClaimantType()).thenReturn(claimantType);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getClaimantCircumstances().getClaimantCircumstancesSelect()).isEqualTo(claimantSelect);
        assertThat(pcsCase.getClaimantCircumstances().getClaimantCircumstancesDetails()).isEqualTo(claimantDetails);

        assertThat(pcsCase.getDefendantCircumstances().getHasDefendantCircumstancesInfo()).isEqualTo(defendantSelect);
        assertThat(pcsCase.getDefendantCircumstances().getDefendantCircumstancesInfo()).isEqualTo(defendantDetails);

        assertThat(pcsCase.getAdditionalReasonsForPossession().getHasReasons()).isEqualTo(additionalReasonsProvided);
        assertThat(pcsCase.getAdditionalReasonsForPossession().getReasons()).isEqualTo(additionalReasons);

        if (claimantType != null) {
            assertThat(pcsCase.getClaimantType().getValue().getCode()).isEqualTo(claimantType.name());
            assertThat(pcsCase.getClaimantType().getValue().getLabel()).isEqualTo(claimantType.getLabel());
        } else {
            assertThat(pcsCase.getClaimantType()).isNull();
        }
    }

    @Test
    void shouldNotPopulateAnyClaimFieldsWhenNoClaimsExist() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertThat(pcsCase.getClaimAgainstTrespassers()).isNull();
        assertThat(pcsCase.getClaimingCostsWanted()).isNull();
        assertThat(pcsCase.getClaimantCircumstances()).isNull();
        assertThat(pcsCase.getDefendantCircumstances()).isNull();
        assertThat(pcsCase.getAdditionalReasonsForPossession()).isNull();
        assertThat(pcsCase.getClaimantType()).isNull();
    }

    private static Stream<Arguments> complexClaimFieldsScenarios() {
        return Stream.of(
            Arguments.of(
                VerticalYesNo.YES, "claimant info",
                VerticalYesNo.NO, "defendant info",
                VerticalYesNo.YES, "some reasons",
                ClaimantType.PRIVATE_LANDLORD
            ),
            Arguments.of(
                null, null,
                VerticalYesNo.NO, "defendant info",
                VerticalYesNo.YES, "some reasons",
                ClaimantType.COMMUNITY_LANDLORD
            ),
            Arguments.of(
                VerticalYesNo.YES, "claimant info",
                null, null,
                VerticalYesNo.NO, null,
                ClaimantType.MORTGAGE_LENDER
            ),
            Arguments.of(
                VerticalYesNo.NO, "some claimant details",
                VerticalYesNo.YES, "some defendant details",
                VerticalYesNo.NO, "reasons",
                null
            )
        );
    }

}
