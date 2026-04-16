package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WarrantOfRestitutionMapperTest {

    private final WarrantOfRestitutionMapper mapper = new WarrantOfRestitutionMapper();

    @Test
    void shouldMapWarrantFieldsToWarrantOfRest() {
        EnforcementOrder source = EnforcementOrder.builder().build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
                .anyRiskToBailiff(YesNoNotSure.YES)
                .propertyAccessDetails(PropertyAccessDetails.builder()
                        .isDifficultToAccessProperty(SimpleYesNo.YES)
                        .clarificationOnAccessDifficultyText("original")
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
        mapper.prePopulateFieldsFromWarrantDetails(source, target);

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
        mapper.prePopulateFieldsFromWarrantDetails(source, target);

        // Then
        assertThat(target.getWarrantOfRestitutionDetails().getRiskCategories()).isNull();
        assertThat(target.getWarrantOfRestitutionDetails().getRiskDetails()).isNull();
        assertThat(target.getWarrantOfRestitutionDetails().getPropertyAccessDetails()).isNull();
        assertThat(target.getRawWarrantRestDetails().getVulnerableAdultsChildrenWarrantRest()).isNull();
        assertThat(target.getRawWarrantRestDetails().getVulnerablePeoplePresentWarrantRest()).isNull();
    }
}
