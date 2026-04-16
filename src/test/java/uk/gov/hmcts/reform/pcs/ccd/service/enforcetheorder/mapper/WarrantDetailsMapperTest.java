package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WarrantDetailsMapperTest {

    @InjectMocks
    private WarrantDetailsMapper underTest;

    private EnforcementOrderEntity enforcementOrderEntity;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = new EnforcementOrderEntity();
    }

    @Test
    void shouldMapToEntityWithNullWarrantDetails() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldMapControlFlags() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(YesOrNo.YES)
            .showPeopleWhoWillBeEvictedPage(YesOrNo.NO)
            .showPeopleYouWantToEvictPage(YesOrNo.YES)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getShowPeopleWhoWillBeEvictedPage()).isEqualTo(SimpleYesNo.NO);
        assertThat(result.getShowPeopleYouWantToEvictPage()).isEqualTo(SimpleYesNo.YES);
    }

    @Test
    void shouldMapControlFlagsWithNullValues() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(null)
            .showPeopleWhoWillBeEvictedPage(null)
            .showPeopleYouWantToEvictPage(null)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getShowPeopleWhoWillBeEvictedPage()).isEqualTo(SimpleYesNo.NO);
        assertThat(result.getShowPeopleYouWantToEvictPage()).isEqualTo(SimpleYesNo.NO);
    }

    @Test
    void shouldMapSuspendTheOrder() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder().isSuspendedOrder(SimpleYesNo.YES).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getIsSuspendedOrder()).isEqualTo(SimpleYesNo.YES);
    }

    @Test
    void shouldMapAdditionalInformation() {
        // Given
        AdditionalInformation additionalInfo = AdditionalInformation.builder()
            .additionalInformationSelect(SimpleYesNo.YES)
            .additionalInformationDetails("Additional details")
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().additionalInformation(additionalInfo).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getAdditionalInformationDetails()).isEqualTo("Additional details");
    }

    @Test
    void shouldHandleNullAdditionalInformation() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .additionalInformation(null)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(warrantDetails)
            .build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isNull();
        assertThat(result.getAdditionalInformationDetails()).isNull();
    }

    @Test
    void shouldMapNameAndAddressForEviction() {
        // Given
        NameAndAddressForEviction nameAndAddress = NameAndAddressForEviction.builder()
            .correctNameAndAddress(SimpleYesNo.NO)
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .nameAndAddressForEviction(nameAndAddress)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(warrantDetails)
            .build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCorrectNameAndAddress()).isEqualTo(SimpleYesNo.NO);
    }

    @Test
    void shouldMapPeopleToEvict() {
        // Given
        PeopleToEvict peopleToEvict = PeopleToEvict.builder().evictEveryone(SimpleYesNo.YES).build();
        WarrantDetails warrantDetails = WarrantDetails.builder().peopleToEvict(peopleToEvict).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getEvictEveryone()).isEqualTo(SimpleYesNo.YES);
    }

    @Test
    void shouldMapPropertyAccessDetails() {
        // Given
        PropertyAccessDetails accessDetails = PropertyAccessDetails.builder()
            .isDifficultToAccessProperty(SimpleYesNo.YES)
            .clarificationOnAccessDifficultyText("Hard to access")
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().propertyAccessDetails(accessDetails).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getIsDifficultToAccessProperty()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getClarificationOnAccessDifficultyText()).isEqualTo("Hard to access");
    }

    @Test
    void shouldMapLegalCosts() {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .areLegalCostsToBeClaimed(SimpleYesNo.YES)
            .amountOfLegalCosts(new BigDecimal("1500.50"))
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().legalCosts(legalCosts).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAreLegalCostsToBeClaimed()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getAmountOfLegalCosts()).isEqualByComparingTo(new BigDecimal("1500.50"));
    }

    @Test
    void shouldHandleLegalCostsWithEmptyAmount() {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .areLegalCostsToBeClaimed(SimpleYesNo.YES)
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().legalCosts(legalCosts).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAreLegalCostsToBeClaimed()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getAmountOfLegalCosts()).isNull();
    }

    @Test
    void shouldMapMoneyOwed() {
        // Given
        MoneyOwedByDefendants moneyOwed = MoneyOwedByDefendants.builder()
            .amountOwed(new BigDecimal("2500.75"))
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().moneyOwedByDefendants(moneyOwed).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAmountOwed()).isEqualByComparingTo(new BigDecimal("2500.75"));
    }

    @Test
    void shouldHandleMoneyOwedWithEmptyAmount() {
        // Given
        MoneyOwedByDefendants moneyOwed = MoneyOwedByDefendants.builder()
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().moneyOwedByDefendants(moneyOwed).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAmountOwed()).isNull();
    }

    @Test
    void shouldMapLandRegistryFees() {
        // Given
        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .haveLandRegistryFeesBeenPaid(SimpleYesNo.YES)
            .amountOfLandRegistryFees(new BigDecimal("350.00"))
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().landRegistryFees(landRegistryFees).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getHaveLandRegistryFeesBeenPaid()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getAmountOfLandRegistryFees()).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    void shouldMapLandRegistryFeesWithEmptyAmount() {
        // Given
        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .haveLandRegistryFeesBeenPaid(SimpleYesNo.YES)
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().landRegistryFees(landRegistryFees).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getHaveLandRegistryFeesBeenPaid()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getAmountOfLandRegistryFees()).isNull();
    }

    @Test
    void shouldMapRepaymentCosts() {
        // Given
        RepaymentPreference repaymentPreference = RepaymentPreference.SOME;

        RepaymentCosts repaymentCosts = RepaymentCosts.builder()
            .repaymentChoice(repaymentPreference)
            .amountOfRepaymentCosts(new BigDecimal("1000.00"))
            .repaymentSummaryMarkdown("Summary markdown")
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().repaymentCosts(repaymentCosts).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getRepaymentChoice()).isEqualTo(RepaymentPreference.SOME.getLabel());
        assertThat(result.getAmountOfRepaymentCosts()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getRepaymentSummaryMarkdown()).isEqualTo("Summary markdown");
    }

    @Test
    void shouldMapDefendantsDOB() {
        // Given
        DefendantsDOB defendantsDOB = DefendantsDOB.builder().defendantsDOBDetails("01/01/1980").build();
        WarrantDetails warrantDetails = WarrantDetails.builder().defendantsDOBKnown(SimpleYesNo.YES)
            .defendantsDOB(defendantsDOB).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantsDOBKnown()).isEqualTo(SimpleYesNo.YES);
        assertThat(result.getDefendantsDOBDetails()).isEqualTo("01/01/1980");
    }

    @Test
    void shouldHandleNullDefendantsDOB() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder().defendantsDOBKnown(SimpleYesNo.NO)
            .defendantsDOB(null).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantsDOBKnown()).isEqualTo(SimpleYesNo.NO);
        assertThat(result.getDefendantsDOBDetails()).isNull();
    }

    @Test
    void shouldMapStatementOfTruth() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.CLAIMANT);
        statementOfTruth.setFullNameClaimant("John Doe");
        statementOfTruth.setPositionClaimant("Owner");
        statementOfTruth.setFullNameLegalRep("Jane Smith");
        statementOfTruth.setFirmNameLegalRep("Smith & Co");
        statementOfTruth.setPositionLegalRep("Solicitor");
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(result.getFullNameClaimant()).isEqualTo("John Doe");
        assertThat(result.getPositionClaimant()).isEqualTo("Owner");
        assertThat(result.getFullNameLegalRep()).isEqualTo("Jane Smith");
        assertThat(result.getFirmNameLegalRep()).isEqualTo("Smith & Co");
        assertThat(result.getPositionLegalRep()).isEqualTo("Solicitor");
    }

    @Test
    void shouldMapAgreementClaimant() {
        // Given
        List<StatementOfTruthAgreementClaimant> agreements = List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE);
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setAgreementClaimant(agreements);
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementClaimant()).isEqualTo(StatementOfTruthAgreementClaimant.BELIEVE_TRUE.name());
    }

    @Test
    void shouldHandleEmptyAgreementClaimant() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setAgreementClaimant(Collections.emptyList());
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementClaimant()).isNull();
    }

    @Test
    void shouldMapAgreementLegalRep() {
        // Given
        List<StatementOfTruthAgreementLegalRep> agreements = List.of(StatementOfTruthAgreementLegalRep.AGREED);
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setAgreementLegalRep(agreements);
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementLegalRep()).isEqualTo(StatementOfTruthAgreementLegalRep.AGREED.name());
    }

    @Test
    void shouldMapCertification() {
        // Given
        List<StatementOfTruthAgreement> certifications = List.of(StatementOfTruthAgreement.CERTIFY);
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCertification(certifications);

        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCertification()).isEqualTo(StatementOfTruthAgreement.CERTIFY.name());
    }

    @Test
    void shouldMapCompleteWarrantDetails() {
        // Given
        EnforcementOrder enforcementOrder = createCompleteEnforcementOrder();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(result.getIsSuspendedOrder()).isEqualTo(SimpleYesNo.NO);
        assertThat(result.getAmountOfLegalCosts()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
    }

    @ParameterizedTest
    @EnumSource(RepaymentPreference.class)
    void shouldMapRepaymentCostsForAllPreferences(RepaymentPreference repaymentPreference) {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(YesOrNo.YES)
            .showPeopleWhoWillBeEvictedPage(YesOrNo.NO)
            .showPeopleYouWantToEvictPage(YesOrNo.YES)
            .build();
        RepaymentCosts repaymentCosts = RepaymentCosts.builder()
            .repaymentChoice(repaymentPreference)
            .amountOfRepaymentCosts(new BigDecimal("500.00"))
            .repaymentSummaryMarkdown("Repayment summary")
            .build();
        warrantDetails.setRepaymentCosts(repaymentCosts);
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity entity = underTest.toEntity(enforcementOrder, new EnforcementOrderEntity());

        // Then
        assertThat(entity.getRepaymentChoice()).isEqualTo(repaymentPreference.getLabel());
        assertThat(entity.getAmountOfRepaymentCosts()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(entity.getRepaymentSummaryMarkdown()).isEqualTo("Repayment summary");
    }

    private EnforcementOrder createCompleteEnforcementOrder() {
        StatementOfTruthDetailsEnforcement statementOfTruth = getStatementOfTruthDetailsEnforcement();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(YesOrNo.YES)
            .isSuspendedOrder(SimpleYesNo.NO)
            .additionalInformation(AdditionalInformation.builder()
                                       .additionalInformationSelect(SimpleYesNo.YES)
                                       .additionalInformationDetails("Details")
                                       .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(SimpleYesNo.YES)
                            .amountOfLegalCosts(new BigDecimal("1000.00"))
                            .build())
            .statementOfTruth(statementOfTruth)
            .build();

        return EnforcementOrder.builder().warrantDetails(warrantDetails).build();
    }

    private static @NotNull StatementOfTruthDetailsEnforcement getStatementOfTruthDetailsEnforcement() {
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        statementOfTruth.setFullNameLegalRep("Legal Rep Name");
        statementOfTruth.setFirmNameLegalRep("Law Firm");
        statementOfTruth.setPositionLegalRep("Senior Partner");
        statementOfTruth.setAgreementLegalRep(List.of(StatementOfTruthAgreementLegalRep.AGREED));
        statementOfTruth.setCertification(List.of(StatementOfTruthAgreement.CERTIFY));
        return statementOfTruth;
    }
}
