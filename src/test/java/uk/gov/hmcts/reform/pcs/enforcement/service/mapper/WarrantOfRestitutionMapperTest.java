package uk.gov.hmcts.reform.pcs.enforcement.service.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;

import java.math.BigDecimal;
import java.util.List;
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
    void shouldKeepTheClaimantsAnswersWhenTheApplicationPageRunsAgain() {
        // Given a claimant who has been seeded from their warrant and then answered the restitution questions
        EnforcementOrder source = warrantEnforcementOrder();
        EnforcementOrder target = EnforcementOrder.builder().build();
        underTest.prePopulateFieldsFromWarrantDetails(source, target);

        WarrantOfRestitutionDetails answers = target.getWarrantOfRestitutionDetails();
        answers.setHowDefendantsReturned("They broke back in through the kitchen window");
        answers.setAdditionalDocuments(List.of(ListValue.<EvidenceOfDefendants>builder()
                .value(EvidenceOfDefendants.builder().description("photograph of the window").build())
                .build()));
        answers.setAnyRiskToBailiff(YesNoNotSure.NO);
        answers.getPropertyAccessDetails().setClarificationOnAccessDifficultyText("the side gate is locked");
        target.getRawWarrantRestDetails().setVulnerablePeoplePresentWarrantRest(YesNoNotSure.NO);

        // When the "Your application" page runs again, as it does on ExUI "Previous" then "Continue"
        underTest.prePopulateFieldsFromWarrantDetails(source, target);

        // Then
        assertThat(target.getWarrantOfRestitutionDetails().getHowDefendantsReturned())
                .isEqualTo("They broke back in through the kitchen window");
        assertThat(target.getWarrantOfRestitutionDetails().getAdditionalDocuments()).hasSize(1);
        assertThat(target.getWarrantOfRestitutionDetails().getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
        assertThat(target.getWarrantOfRestitutionDetails().getPropertyAccessDetails()
                .getClarificationOnAccessDifficultyText()).isEqualTo("the side gate is locked");
        assertThat(target.getRawWarrantRestDetails().getVulnerablePeoplePresentWarrantRest())
                .isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldKeepTheAnswersOfAResumedDraftApplication() {
        // Given a draft the claimant saved after answering the restitution questions
        EnforcementOrder source = warrantEnforcementOrder();
        EnforcementOrder target = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .howDefendantsReturned("They broke back in through the kitchen window")
                        .anyRiskToBailiff(YesNoNotSure.NO)
                        .build())
                .build();

        // When the resumed draft continues off the "Your application" page
        underTest.prePopulateFieldsFromWarrantDetails(source, target);

        // Then
        assertThat(target.getWarrantOfRestitutionDetails().getHowDefendantsReturned())
                .isEqualTo("They broke back in through the kitchen window");
        assertThat(target.getWarrantOfRestitutionDetails().getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
        assertThat(target.getWarrantOfRestitutionDetails().getRiskCategories()).isNull();
        assertThat(target.getRawWarrantRestDetails()).isNull();
    }

    @Test
    void shouldSeedTheAnswersWhenOnlyTheEmptyContainersArePresent() {
        // Given the containers CCD sends back with no answer in them
        EnforcementOrder source = warrantEnforcementOrder();
        EnforcementOrder target = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(new WarrantOfRestitutionDetails())
                .rawWarrantRestDetails(new RawWarrantRestDetails())
                .build();

        // When
        underTest.prePopulateFieldsFromWarrantDetails(source, target);

        // Then
        assertThat(target.getWarrantOfRestitutionDetails().getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.YES);
        assertThat(target.getRawWarrantRestDetails().getVulnerablePeoplePresentWarrantRest())
                .isEqualTo(YesNoNotSure.YES);
    }

    private static EnforcementOrder warrantEnforcementOrder() {
        return EnforcementOrder.builder()
                .warrantDetails(WarrantDetails.builder()
                        .anyRiskToBailiff(YesNoNotSure.YES)
                        .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                        .riskDetails(RiskDetails.builder().violentDetails("violent").build())
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .isDifficultToAccessProperty(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText("communal entrance")
                                .build())
                        .additionalInformation(AdditionalInformation.builder()
                                .additionalInformationSelect(VerticalYesNo.NO)
                                .build())
                        .build())
                .rawWarrantDetails(RawWarrantDetails.builder()
                        .vulnerablePeoplePresent(YesNoNotSure.YES)
                        .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                                .vulnerableReasonText("reason")
                                .build())
                        .build())
                .build();
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

    /**
     * The journey asks which language the claimant used on the warrant journey's page (HDPI-7555).
     */
    @Test
    void shouldMapTheLanguageAnsweredOnTheWarrantPage() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantDetails(WarrantDetails.builder().languageUsed(LanguageUsed.ENGLISH_AND_WELSH).build())
                .build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH_AND_WELSH);
    }

    @Test
    void shouldMapNoLanguageWhenTheApplicationNeverReachedThePage() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();

        // When
        WarrantOfRestitutionEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getLanguageUsed()).isNull();
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
