package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementRiskProfileMapperTest {

    private final EnforcementRiskProfileMapper mapper =
            new EnforcementRiskProfileMapper(new ModelMapper());

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("maps full warrant and risk details to entity")
        void mapsFullWarrantAndRiskDetails() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                            .anyRiskToBailiff(YesNoNotSure.YES)
                            .riskDetails(EnforcementRiskDetails.builder()
                                    .violentDetails("Violent")
                                    .verbalThreatsDetails("Verbal")
                                    .firearmsDetails("Firearms")
                                    .build())
                            .build())
                    .build();

            EnforcementRiskProfileEntity result = mapper.toEntity(orderEntity, order);

            assertThat(result.getEnforcementOrder()).isEqualTo(orderEntity);
            assertThat(result.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.YES);
            assertThat(result.getViolentDetails()).isEqualTo("Violent");
            assertThat(result.getVerbalThreatsDetails()).isEqualTo("Verbal");
            assertThat(result.getFirearmsDetails()).isEqualTo("Firearms");
        }

        @Test
        @DisplayName("maps vulnerability details from raw warrant details")
        void mapsVulnerabilityDetails() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder().build())
                    .rawWarrantDetails(RawWarrantDetails.builder()
                            .vulnerablePeoplePresent(YesNoNotSure.YES)
                            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                                    .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                                    .vulnerableReasonText("Vulnerability reason")
                                    .build())
                            .build())
                    .build();

            EnforcementRiskProfileEntity result = mapper.toEntity(orderEntity, order);

            assertThat(result.getVulnerablePeoplePresent()).isEqualTo(YesNoNotSure.YES);
            assertThat(result.getVulnerableCategory()).isEqualTo(VulnerableCategory.VULNERABLE_ADULTS);
            assertThat(result.getVulnerableReasonText()).isEqualTo("Vulnerability reason");
        }

        @Test
        @DisplayName("returns entity with null risk fields when warrant and raw details are null")
        void returnsEntityWithNullRiskFieldsWhenDetailsNull() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder().build();

            EnforcementRiskProfileEntity result = mapper.toEntity(orderEntity, order);

            assertThat(result.getEnforcementOrder()).isEqualTo(orderEntity);
            assertThat(result.getAnyRiskToBailiff()).isNull();
            assertThat(result.getViolentDetails()).isNull();
            assertThat(result.getVulnerablePeoplePresent()).isNull();
            assertThat(result.getVulnerableCategory()).isNull();
            assertThat(result.getVulnerableReasonText()).isNull();
        }

        @Test
        @DisplayName("maps warrant details when risk details is null")
        void mapsWarrantDetailsWhenRiskDetailsNull() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                            .anyRiskToBailiff(YesNoNotSure.NO)
                            .build())
                    .build();

            EnforcementRiskProfileEntity result = mapper.toEntity(orderEntity, order);

            assertThat(result.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
            assertThat(result.getViolentDetails()).isNull();
            assertThat(result.getVerbalThreatsDetails()).isNull();
        }
    }
}
