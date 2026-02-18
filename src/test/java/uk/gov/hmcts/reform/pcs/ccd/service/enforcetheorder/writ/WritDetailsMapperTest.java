package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.writ.EnforcementWritEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("WritDetailsMapper Tests")
class WritDetailsMapperTest {

    @InjectMocks
    private WritDetailsMapper underTest;

    private WritDetails writDetails;
    private NameAndAddressForEviction nameAndAddressForEviction;
    private LandRegistryFees landRegistryFees;
    private LegalCosts legalCosts;
    private MoneyOwedByDefendants moneyOwedByDefendants;

    @BeforeEach
    void setUp() {
        nameAndAddressForEviction = NameAndAddressForEviction.builder()
            .correctNameAndAddress(VerticalYesNo.YES)
            .build();

        landRegistryFees = LandRegistryFees.builder()
            .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
            .amountOfLandRegistryFees(new BigDecimal("500.00"))
            .build();

        legalCosts = LegalCosts.builder()
            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
            .amountOfLegalCosts(new BigDecimal("1500.00"))
            .build();

        moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(new BigDecimal("3000.00"))
            .build();

        writDetails = WritDetails.builder()
            .nameAndAddressForEviction(nameAndAddressForEviction)
            .showChangeNameAddressPage(YesOrNo.NO)
            .showPeopleWhoWillBeEvictedPage(YesOrNo.YES)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.YES)
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
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isEqualTo(VerticalYesNo.YES);
        assertThat(entity.getShowPeopleWhoWillBeEvictedPage()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.YES);
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isEqualTo(VerticalYesNo.YES);
        assertThat(entity.getAmountOfLandRegistryFees()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(entity.getAreLegalCostsToBeClaimed()).isEqualTo(VerticalYesNo.YES);
        assertThat(entity.getAmountOfLegalCosts()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(entity.getAmountOwed()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void shouldHandleNullNameAndAddressForEviction() {
        // Given
        writDetails.setNameAndAddressForEviction(null);

        // When
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    void shouldHandleNullLandRegistryFees() {
        // Given
        writDetails.setLandRegistryFees(null);

        // When
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

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
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

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
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

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
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

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
                                           .correctNameAndAddress(VerticalYesNo.NO)
                                           .build())
            .showChangeNameAddressPage(YesOrNo.NO)
            .showPeopleWhoWillBeEvictedPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.NO)
            .landRegistryFees(LandRegistryFees.builder()
                                  .haveLandRegistryFeesBeenPaid(VerticalYesNo.NO)
                                  .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(VerticalYesNo.NO)
                            .build())
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder().build())
            .build();

        // When
        EnforcementWritEntity entity = underTest.toEntity(noWritDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getShowPeopleWhoWillBeEvictedPage()).isEqualTo(YesOrNo.NO);
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.NO);
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getAreLegalCostsToBeClaimed()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldMapWithZeroAmounts() {
        // Given
        WritDetails zeroAmountWritDetails = WritDetails.builder()
            .nameAndAddressForEviction(nameAndAddressForEviction)
            .landRegistryFees(LandRegistryFees.builder()
                                  .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
                                  .amountOfLandRegistryFees(BigDecimal.ZERO)
                                  .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
                            .amountOfLegalCosts(BigDecimal.ZERO)
                            .build())
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder()
                                       .amountOwed(BigDecimal.ZERO)
                                       .build())
            .build();

        // When
        EnforcementWritEntity entity = underTest.toEntity(zeroAmountWritDetails);

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
                                  .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
                                  .amountOfLandRegistryFees(largeAmount)
                                  .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
                            .amountOfLegalCosts(largeAmount)
                            .build())
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder()
                                       .amountOwed(largeAmount)
                                       .build())
            .build();

        // When
        EnforcementWritEntity entity = underTest.toEntity(largeAmountWritDetails);

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
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHceoDetails()).hasSize(120);
        assertThat(entity.getHceoDetails()).isEqualTo(maxLengthHceoDetails);
    }

    @Test
    void shouldMapWithNullHceoDetails() {
        // Given
        writDetails.setHceoDetails(null);

        // When
        EnforcementWritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHceoDetails()).isNull();
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldCreateNewEntityInstanceOnEachCall() {
        // When
        EnforcementWritEntity entity1 = underTest.toEntity(writDetails);
        EnforcementWritEntity entity2 = underTest.toEntity(writDetails);

        // Then
        assertThat(entity1).isNotSameAs(entity2);
        assertThat(entity1.getHceoDetails()).isEqualTo(entity2.getHceoDetails());
    }

    @Test
    void shouldMapPartialDataCorrectly() {
        // Given
        WritDetails partialWritDetails = WritDetails.builder()
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .moneyOwedByDefendants(MoneyOwedByDefendants.builder()
                                       .amountOwed(new BigDecimal("2000.00"))
                                       .build())
            .build();

        // When
        EnforcementWritEntity entity = underTest.toEntity(partialWritDetails);

        // Then
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getAmountOwed()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHaveLandRegistryFeesBeenPaid()).isNull();
        assertThat(entity.getAreLegalCostsToBeClaimed()).isNull();
    }
}
