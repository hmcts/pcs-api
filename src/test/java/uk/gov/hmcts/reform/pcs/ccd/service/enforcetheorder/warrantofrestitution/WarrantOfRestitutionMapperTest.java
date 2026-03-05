package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrantofrestitution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT_OF_RESTITUTION;

@ExtendWith(MockitoExtension.class)
class WarrantOfRestitutionMapperTest {

    @InjectMocks
    private WarrantOfRestitutionMapper warrantOfRestitutionMapper;

    @Test
    void shouldPrepopulateFieldsFromEnforcementOrder() {
        // Given
        EnforcementOrder warrantOrder = EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType(WARRANT);
        warrantOrder.setRawWarrantDetails(RawWarrantDetails.builder()
            .vulnerablePeoplePresent(YesNoNotSure.YES)
            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                    .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS_AND_CHILDREN)
                    .vulnerableReasonText("Vulnerability reason")
                    .build())
            .build());
        warrantOrder.setWarrantDetails(WarrantDetails.builder()
            .anyRiskToBailiff(YesNoNotSure.YES)
            .propertyAccessDetails(PropertyAccessDetails.builder()
                    .isDifficultToAccessProperty(VerticalYesNo.NO)
                    .build())
            .build());

        EnforcementOrder enforcementOrder =
                EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType(WARRANT_OF_RESTITUTION);

        // When
        warrantOfRestitutionMapper.prePopulateFieldsFromWarrantDetails(warrantOrder, enforcementOrder);

        // Then - use AssertJ correctly: assert actual values equal expected values
        assertThat(enforcementOrder.getRawWarrantRestDetails().getVulnerablePeoplePresent())
                .isEqualTo(warrantOrder.getRawWarrantDetails().getVulnerablePeoplePresent());

        assertThat(enforcementOrder.getRawWarrantRestDetails().getVulnerableAdultsChildren().getVulnerableCategory())
                .isEqualTo(warrantOrder.getRawWarrantDetails().getVulnerableAdultsChildren().getVulnerableCategory());

        assertThat(enforcementOrder.getRawWarrantRestDetails().getVulnerableAdultsChildren().getVulnerableReasonText())
                .isEqualTo(warrantOrder.getRawWarrantDetails().getVulnerableAdultsChildren().getVulnerableReasonText());

        assertThat(enforcementOrder.getWarrantOfRestitutionDetails().getAnyRiskToBailiff())
                .isEqualTo(warrantOrder.getWarrantDetails().getAnyRiskToBailiff());

        // PropertyAccessDetails should be equal in content but a different instance (defensive copy)
        assertThat(enforcementOrder.getWarrantOfRestitutionDetails().getPropertyAccessDetails())
                .isEqualTo(warrantOrder.getWarrantDetails().getPropertyAccessDetails());
        assertThat(enforcementOrder.getWarrantOfRestitutionDetails().getPropertyAccessDetails())
                .isNotSameAs(warrantOrder.getWarrantDetails().getPropertyAccessDetails());
    }
}