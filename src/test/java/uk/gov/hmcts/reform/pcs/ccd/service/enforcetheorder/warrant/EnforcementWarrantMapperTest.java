package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementWarrantMapperTest {

    @InjectMocks
    private EnforcementWarrantMapper mapper;

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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getShowChangeNameAddressPage()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getShowPeopleWhoWillBeEvictedPage()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getShowPeopleYouWantToEvictPage()).isEqualTo(VerticalYesNo.YES);
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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getShowChangeNameAddressPage()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getShowPeopleWhoWillBeEvictedPage()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getShowPeopleYouWantToEvictPage()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldMapSuspendTheOrder() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder().isSuspendedOrder(VerticalYesNo.YES).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getIsSuspendedOrder()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldMapAdditionalInformation() {
        // Given
        AdditionalInformation additionalInfo = AdditionalInformation.builder()
            .additionalInformationSelect(VerticalYesNo.YES)
            .additionalInformationDetails("Additional details")
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().additionalInformation(additionalInfo).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isEqualTo(VerticalYesNo.YES);
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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isNull();
        assertThat(result.getAdditionalInformationDetails()).isNull();
    }

    @Test
    void shouldMapNameAndAddressForEviction() {
        // Given
        NameAndAddressForEviction nameAndAddress = NameAndAddressForEviction.builder()
            .correctNameAndAddress(VerticalYesNo.NO)
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .nameAndAddressForEviction(nameAndAddress)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(warrantDetails)
            .build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCorrectNameAndAddress()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldMapPeopleToEvict() {
        // Given
        PeopleToEvict peopleToEvict = PeopleToEvict.builder().evictEveryone(VerticalYesNo.YES).build();
        WarrantDetails warrantDetails = WarrantDetails.builder().peopleToEvict(peopleToEvict).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getEvictEveryone()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldMapPropertyAccessDetails() {
        // Given
        PropertyAccessDetails accessDetails = PropertyAccessDetails.builder()
            .isDifficultToAccessProperty(VerticalYesNo.YES)
            .clarificationOnAccessDifficultyText("Hard to access")
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().propertyAccessDetails(accessDetails).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getIsDifficultToAccessProperty()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getClarificationOnAccessDifficultyText()).isEqualTo("Hard to access");
    }

    @Test
    void shouldMapLegalCosts() {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
            .amountOfLegalCosts(new BigDecimal("1500.50"))
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().legalCosts(legalCosts).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAreLegalCostsToBeClaimed()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getAmountOfLegalCosts()).isEqualByComparingTo(new BigDecimal("1500.50"));
    }

    @Test
    void shouldHandleLegalCostsWithEmptyAmount() {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().legalCosts(legalCosts).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAreLegalCostsToBeClaimed()).isEqualTo(VerticalYesNo.YES);
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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAmountOwed()).isNull();
    }

    @Test
    void shouldMapLandRegistryFees() {
        // Given
        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
            .amountOfLandRegistryFees(new BigDecimal("350.00"))
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().landRegistryFees(landRegistryFees).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getHaveLandRegistryFeesBeenPaid()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getAmountOfLandRegistryFees()).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    void shouldMapLandRegistryFeesWithEmptyAmount() {
        // Given
        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().landRegistryFees(landRegistryFees).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getHaveLandRegistryFeesBeenPaid()).isEqualTo(VerticalYesNo.YES);
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
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getRepaymentChoice()).isEqualTo(RepaymentPreference.SOME.getLabel());
        assertThat(result.getAmountOfRepaymentCosts()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getRepaymentSummaryMarkdown()).isEqualTo("Summary markdown");
    }

    @Test
    void shouldHandleRepaymentCostsWithNullChoice() {
        // Given
        RepaymentCosts repaymentCosts = RepaymentCosts.builder()
            .repaymentChoice(null)
            .amountOfRepaymentCosts(new BigDecimal("1000.00"))
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().repaymentCosts(repaymentCosts).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getRepaymentChoice()).isNull();
        assertThat(result.getAmountOfRepaymentCosts()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void shouldMapDefendantsDOB() {
        // Given
        DefendantsDOB defendantsDOB = DefendantsDOB.builder().defendantsDOBDetails("01/01/1980").build();
        WarrantDetails warrantDetails = WarrantDetails.builder().defendantsDOBKnown(VerticalYesNo.YES)
            .defendantsDOB(defendantsDOB).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantsDOBKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getDefendantsDOBDetails()).isEqualTo("01/01/1980");
    }

    @Test
    void shouldHandleNullDefendantsDOB() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder().defendantsDOBKnown(VerticalYesNo.NO)
            .defendantsDOB(null).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantsDOBKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getDefendantsDOBDetails()).isNull();
    }

    @Test
    void shouldMapRiskAssessment() {
        // Given
        Set<RiskCategory> riskCategories = Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.AGENCY_VISITS);
        WarrantDetails warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.YES)
            .enforcementRiskCategories(riskCategories).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getEnforcementRiskCategories()).contains(RiskCategory.VIOLENT_OR_AGGRESSIVE.name(),
                                                                   RiskCategory.AGENCY_VISITS.name());
    }

    @Test
    void shouldHandleNullRiskCategories() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.NO)
            .enforcementRiskCategories(null).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getEnforcementRiskCategories()).isNull();
    }

    @Test
    void shouldMapStatementOfTruth() {
        // Given
        StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder()
            .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
            .fullNameClaimant("John Doe")
            .positionClaimant("Owner")
            .fullNameLegalRep("Jane Smith")
            .firmNameLegalRep("Smith & Co")
            .positionLegalRep("Solicitor")
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

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
        StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder().agreementClaimant(agreements)
            .build();
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementClaimant()).isEqualTo(StatementOfTruthAgreementClaimant.BELIEVE_TRUE.name());
    }

    @Test
    void shouldHandleEmptyAgreementClaimant() {
        // Given
        StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder()
            .agreementClaimant(Collections.emptyList()).build();
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementClaimant()).isNull();
    }

    @Test
    void shouldMapAgreementLegalRep() {
        // Given
        List<StatementOfTruthAgreementLegalRep> agreements = List.of(StatementOfTruthAgreementLegalRep.AGREED);
        StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder()
            .agreementLegalRep(agreements).build();
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementLegalRep()).isEqualTo(StatementOfTruthAgreementLegalRep.AGREED.name());
    }

    @Test
    void shouldMapCertification() {
        // Given
        List<StatementOfTruthAgreement> certifications = List.of(StatementOfTruthAgreement.CERTIFY);
        StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder()
            .certification(certifications)
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCertification()).isEqualTo(StatementOfTruthAgreement.CERTIFY.name());
    }

    @Test
    void shouldMapCompleteWarrantDetails() {
        // Given
        EnforcementOrder enforcementOrder = createCompleteEnforcementOrder();

        // When
        EnforcementWarrantEntity result = mapper.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(result.getShowChangeNameAddressPage()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getIsSuspendedOrder()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getAmountOfLegalCosts()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
    }

    private EnforcementOrder createCompleteEnforcementOrder() {
        StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder()
            .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
            .fullNameLegalRep("Legal Rep Name")
            .firmNameLegalRep("Law Firm")
            .positionLegalRep("Senior Partner")
            .agreementLegalRep(List.of(StatementOfTruthAgreementLegalRep.AGREED))
            .certification(List.of(StatementOfTruthAgreement.CERTIFY))
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(YesOrNo.YES)
            .isSuspendedOrder(VerticalYesNo.NO)
            .additionalInformation(AdditionalInformation.builder()
                                       .additionalInformationSelect(VerticalYesNo.YES)
                                       .additionalInformationDetails("Details")
                                       .build())
            .legalCosts(LegalCosts.builder()
                            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
                            .amountOfLegalCosts(new BigDecimal("1000.00"))
                            .build())
            .statementOfTruth(statementOfTruth)
            .build();

        return EnforcementOrder.builder().warrantDetails(warrantDetails).build();
    }
}
