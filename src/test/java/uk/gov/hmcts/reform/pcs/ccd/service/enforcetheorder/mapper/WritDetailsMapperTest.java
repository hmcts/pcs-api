package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("WritDetailsMapper Tests")
class WritDetailsMapperTest {

    @InjectMocks
    private WritDetailsMapper underTest;

    private WritDetails writDetails;
    private NameAndAddressForEviction nameAndAddressForEviction;

    @BeforeEach
    void setUp() {
        nameAndAddressForEviction = NameAndAddressForEviction.builder()
            .correctNameAndAddress(SimpleYesNo.YES)
            .build();

        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .haveLandRegistryFeesBeenPaid(SimpleYesNo.YES)
            .amountOfLandRegistryFees(new BigDecimal("500.00"))
            .build();

        LegalCosts legalCosts = LegalCosts.builder()
            .areLegalCostsToBeClaimed(SimpleYesNo.YES)
            .amountOfLegalCosts(new BigDecimal("1500.00"))
            .build();

        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(new BigDecimal("3000.00"))
            .build();

        writDetails = WritDetails.builder()
            .nameAndAddressForEviction(nameAndAddressForEviction)
            .showChangeNameAddressPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(SimpleYesNo.YES)
            .hceoDetails("John Smith, Enforcement Ltd")
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .landRegistryFees(landRegistryFees)
            .legalCosts(legalCosts)
            .moneyOwedByDefendants(moneyOwedByDefendants)
            .build();
    }

    @Test
    void shouldMapAllFieldsSuccessfully() {
        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isEqualTo(SimpleYesNo.YES);
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(SimpleYesNo.YES);
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isEqualTo(SimpleYesNo.YES);
        assertThat(entity.getAmountOfLandRegistryFees()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(entity.getAreLegalCostsToBeClaimed()).isEqualTo(SimpleYesNo.YES);
        assertThat(entity.getAmountOfLegalCosts()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(entity.getAmountOwed()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void shouldHandleNullNameAndAddressForEviction() {
        // Given
        writDetails.setNameAndAddressForEviction(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    void shouldHandleNullLandRegistryFees() {
        // Given
        writDetails.setLandRegistryFees(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isNull();
        assertThat(entity.getAmountOfLandRegistryFees()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    void shouldHandleNullLegalCosts() {
        // Given
        writDetails.setLegalCosts(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getAreLegalCostsToBeClaimed()).isNull();
        assertThat(entity.getAmountOfLegalCosts()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    void shouldHandleNullMoneyOwedByDefendants() {
        // Given
        writDetails.setMoneyOwedByDefendants(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getAmountOwed()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    void shouldHandleAllNullCompositeObjects() {
        // Given
        writDetails.setNameAndAddressForEviction(null);
        writDetails.setLandRegistryFees(null);
        writDetails.setLegalCosts(null);
        writDetails.setMoneyOwedByDefendants(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isNull();
        assertThat(entity.getAmountOfLandRegistryFees()).isNull();
        assertThat(entity.getAreLegalCostsToBeClaimed()).isNull();
        assertThat(entity.getAmountOfLegalCosts()).isNull();
        assertThat(entity.getAmountOwed()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    @DisplayName("Should map with all NO/false values")
    void shouldMapWithAllNoValues() {
        // Given
        WritDetails noWritDetails = WritDetails.builder()
            .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                           .correctNameAndAddress(SimpleYesNo.NO)
                                           .build())
            .showChangeNameAddressPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(SimpleYesNo.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.NO)
            .landRegistryFees(LandRegistryFees.builder()
                                  .haveLandRegistryFeesBeenPaid(SimpleYesNo.NO)
                                  .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(SimpleYesNo.NO)
                            .build())
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder().build())
            .build();

        // When
        WritEntity entity = underTest.toEntity(noWritDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isEqualTo(SimpleYesNo.NO);
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(SimpleYesNo.NO);
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.NO);
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isEqualTo(SimpleYesNo.NO);
        assertThat(entity.getAreLegalCostsToBeClaimed()).isEqualTo(SimpleYesNo.NO);
    }

    @Test
    void shouldMapWithZeroAmounts() {
        // Given
        WritDetails zeroAmountWritDetails = WritDetails.builder()
            .nameAndAddressForEviction(nameAndAddressForEviction)
            .landRegistryFees(LandRegistryFees.builder()
                                  .haveLandRegistryFeesBeenPaid(SimpleYesNo.YES)
                                  .amountOfLandRegistryFees(BigDecimal.ZERO)
                                  .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(SimpleYesNo.YES)
                            .amountOfLegalCosts(BigDecimal.ZERO)
                            .build())
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder()
                                       .amountOwed(BigDecimal.ZERO)
                                       .build())
            .build();

        // When
        WritEntity entity = underTest.toEntity(zeroAmountWritDetails);

        // Then
        assertThat(entity.getAmountOfLandRegistryFees()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(entity.getAmountOfLegalCosts()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(entity.getAmountOwed()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldMapWithLargeAmounts() {
        // Given
        BigDecimal largeAmount = new BigDecimal("999999.99");
        WritDetails largeAmountWritDetails = WritDetails.builder()
            .nameAndAddressForEviction(nameAndAddressForEviction)
            .landRegistryFees(LandRegistryFees.builder()
                                  .haveLandRegistryFeesBeenPaid(SimpleYesNo.YES)
                                  .amountOfLandRegistryFees(largeAmount)
                                  .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(SimpleYesNo.YES)
                            .amountOfLegalCosts(largeAmount)
                            .build())
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder()
                                       .amountOwed(largeAmount)
                                       .build())
            .build();

        // When
        WritEntity entity = underTest.toEntity(largeAmountWritDetails);

        // Then
        assertThat(entity.getAmountOfLandRegistryFees()).isEqualByComparingTo(largeAmount);
        assertThat(entity.getAmountOfLegalCosts()).isEqualByComparingTo(largeAmount);
        assertThat(entity.getAmountOwed()).isEqualByComparingTo(largeAmount);
    }

    @Test
    void shouldMapWithMaxLengthHceoDetails() {
        // Given
        String maxLengthHceoDetails = "A".repeat(120);
        writDetails.setHceoDetails(maxLengthHceoDetails);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHceoDetails()).hasSize(120);
        assertThat(entity.getHceoDetails()).isEqualTo(maxLengthHceoDetails);
    }

    @Test
    void shouldMapWithNullHceoDetails() {
        // Given
        writDetails.setHceoDetails(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHceoDetails()).isNull();
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(SimpleYesNo.YES);
    }

    @Test
    void shouldCreateNewEntityInstanceOnEachCall() {
        // When
        WritEntity entity1 = underTest.toEntity(writDetails);
        WritEntity entity2 = underTest.toEntity(writDetails);

        // Then
        assertThat(entity1).isNotSameAs(entity2);
        assertThat(entity1.getHceoDetails()).isEqualTo(entity2.getHceoDetails());
    }

    @Test
    void shouldMapPartialDataCorrectly() {
        // Given
        WritDetails partialWritDetails = WritDetails.builder()
            .hasHiredHighCourtEnforcementOfficer(SimpleYesNo.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder()
                                       .amountOwed(new BigDecimal("2000.00"))
                                       .build())
            .build();

        // When
        WritEntity entity = underTest.toEntity(partialWritDetails);

        // Then
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(SimpleYesNo.NO);
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getAmountOwed()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isNull();
        assertThat(entity.getAreLegalCostsToBeClaimed()).isNull();
    }

    @Test
    void shouldMapLanguageUsed() {
        // Given
        writDetails.setLanguageUsed(LanguageUsed.ENGLISH);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
    }

    @Test
    void shouldMapRepaymentCostsAllNull() {
        // Given
        writDetails.setRepaymentCosts(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getRepaymentChoice()).isNull();
        assertThat(entity.getAmountOfRepaymentCosts()).isNull();
        assertThat(entity.getRepaymentSummaryMarkdown()).isNull();
    }

    @ParameterizedTest
    @EnumSource(RepaymentPreference.class)
    void shouldMapRepaymentCostsForAllPreferences(RepaymentPreference repaymentPreference) {
        // Given
        RepaymentCosts repaymentCosts = RepaymentCosts.builder()
            .repaymentChoice(repaymentPreference)
            .amountOfRepaymentCosts(new BigDecimal("500.00"))
            .repaymentSummaryMarkdown("Repayment summary")
            .build();
        writDetails.setRepaymentCosts(repaymentCosts);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getRepaymentChoice()).isEqualTo(repaymentPreference.getLabel());
        assertThat(entity.getAmountOfRepaymentCosts()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(entity.getRepaymentSummaryMarkdown()).isEqualTo("Repayment summary");
    }

}
