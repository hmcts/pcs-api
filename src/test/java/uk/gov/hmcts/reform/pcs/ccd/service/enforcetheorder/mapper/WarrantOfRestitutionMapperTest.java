package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WarrantOfRestitutionMapperTest {

    private final WarrantOfRestitutionMapper underTest = new WarrantOfRestitutionMapper();
    private EnforcementOrderEntity enforcementOrderEntity;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = new EnforcementOrderEntity();
    }

    @Test
    void shouldMapWarrantFieldsToWarrantOfRest() {
        EnforcementOrder source = EnforcementOrder.builder().build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
                .anyRiskToBailiff(YesNoNotSure.YES)
                .propertyAccessDetails(PropertyAccessDetails.builder()
                        .isDifficultToAccessProperty(VerticalYesNo.YES)
                        .clarificationOnAccessDifficultyText("original")
                        .build())
                .additionalInformation(AdditionalInformation.builder()
                        .additionalInformationDetails("additional details")
                        .additionalInformationSelect(VerticalYesNo.YES)
                        .build())
                .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(RiskDetails.builder()
                        .violentDetails("violent")
                        .verbalThreatsDetails("verbal")
                        .build())
                .build();

        RawWarrantDetails rawWarrantDetails = RawWarrantDetails.builder()
                .vulnerablePeoplePresent(YesNoNotSure.YES)
                .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                        .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                        .vulnerableReasonText("reason")
                        .build())
                .build();

        source.setWarrantDetails(warrantDetails);
        source.setRawWarrantDetails(rawWarrantDetails);

        EnforcementOrder target = EnforcementOrder.builder().build();

        // When
        underTest.prePopulateFieldsFromWarrantDetails(source, target);

        // Then - values copied
        assertThat(target.getWarrantOfRestitutionDetails()).isNotNull();
        assertThat(target.getWarrantOfRestitutionDetails().getAnyRiskToBailiff())
                .isEqualTo(source.getWarrantDetails().getAnyRiskToBailiff());

        // nested object copied and not same instance
        assertThat(target.getWarrantOfRestitutionDetails().getPropertyAccessDetails())
                .isEqualTo(source.getWarrantDetails().getPropertyAccessDetails());
        assertThat(target.getWarrantOfRestitutionDetails().getPropertyAccessDetails())
                .isNotSameAs(source.getWarrantDetails().getPropertyAccessDetails());

        assertThat(target.getWarrantOfRestitutionDetails().getRiskCategories())
                .isEqualTo(source.getWarrantDetails().getRiskCategories());
        assertThat(target.getWarrantOfRestitutionDetails().getRiskCategories())
                .isNotSameAs(source.getWarrantDetails().getRiskCategories());

        assertThat(target.getRawWarrantRestDetails()).isNotNull();

        target.getWarrantOfRestitutionDetails().getPropertyAccessDetails()
                .setClarificationOnAccessDifficultyText("changed");
        assertThat(source.getWarrantDetails().getPropertyAccessDetails().getClarificationOnAccessDifficultyText())
                .isEqualTo("original");
    }

    @Test
    void shouldHandleNullFields() {
        EnforcementOrder source = EnforcementOrder.builder().build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
                .anyRiskToBailiff(YesNoNotSure.YES)
                .build();

        RawWarrantDetails rawWarrantDetails = RawWarrantDetails.builder()
                .build();

        source.setWarrantDetails(warrantDetails);
        source.setRawWarrantDetails(rawWarrantDetails);

        EnforcementOrder target = EnforcementOrder.builder().build();

        // When
        underTest.prePopulateFieldsFromWarrantDetails(source, target);

        // Then
        assertThat(target.getWarrantOfRestitutionDetails().getRiskCategories()).isNull();
        assertThat(target.getWarrantOfRestitutionDetails().getRiskDetails()).isNull();
        assertThat(target.getWarrantOfRestitutionDetails().getPropertyAccessDetails()).isNull();
        assertThat(target.getRawWarrantRestDetails().getVulnerableAdultsChildrenWarrantRest()).isNull();
        assertThat(target.getRawWarrantRestDetails().getVulnerablePeoplePresentWarrantRest()).isNull();
    }

    @Test
    void shouldMapToEntityWithNullWarrantOfRestitutionDetails() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldMapMapAdditionalInformation() {
        // Given
        AdditionalInformation additionalInfo = AdditionalInformation.builder()
                .additionalInformationSelect(VerticalYesNo.YES)
                .additionalInformationDetails("Additional details")
                .build();

        WarrantOfRestitutionDetails warrantOfRestitutionDetails = WarrantOfRestitutionDetails.builder()
                .additionalInformation(additionalInfo).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(warrantOfRestitutionDetails).build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getAdditionalInformationDetails()).isEqualTo("Additional details");
    }

    @Test
    void shouldHandleNullMapAdditionalInformation() {
        // Given
        WarrantOfRestitutionDetails warrantOfRestitutionDetails = WarrantOfRestitutionDetails.builder()
                .additionalInformation(null)
                .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(warrantOfRestitutionDetails)
                .build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isNull();
        assertThat(result.getAdditionalInformationDetails()).isNull();
    }

    @Test
    void shouldMapDefendantReturned() {
        // Given
        WarrantOfRestitutionDetails warrantOfRestitutionDetails = WarrantOfRestitutionDetails.builder()
                .howDefendantsReturned("defendants returned test").build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(warrantOfRestitutionDetails).build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getHowDefendantsReturned()).isEqualTo("defendants returned test");
    }

    @Test
    void shouldMapMapPropertyAccessDetails() {
        // Given
        PropertyAccessDetails accessDetails = PropertyAccessDetails.builder()
                .isDifficultToAccessProperty(VerticalYesNo.YES)
                .clarificationOnAccessDifficultyText("Hard to access")
                .build();

        WarrantOfRestitutionDetails warrantOfRestitutionDetails = WarrantOfRestitutionDetails.builder()
                .propertyAccessDetails(accessDetails).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(warrantOfRestitutionDetails).build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getIsDifficultToAccessProperty()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getClarificationOnAccessDifficultyText()).isEqualTo("Hard to access");
    }

    @Test
    void shouldMapCompleteWarrantDetails() {
        // Given
        EnforcementOrder enforcementOrder = createCompleteEnforcementOrder();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    private EnforcementOrder createCompleteEnforcementOrder() {
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
                .build();

        return EnforcementOrder.builder().warrantDetails(warrantDetails).build();
    }
}