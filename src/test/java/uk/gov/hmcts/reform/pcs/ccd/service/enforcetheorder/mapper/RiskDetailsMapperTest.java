package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementTypes;

@ExtendWith(MockitoExtension.class)
class RiskDetailsMapperTest {

    private final RiskDetailsMapper mapper =
            new RiskDetailsMapper(new ModelMapper());

    @Nested
    @DisplayName("warrant")
    class Warrant {

        @Test
        @DisplayName("maps full warrant and risk details to entity")
        void mapsFullWarrantAndRiskDetails() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .chooseEnforcementType(buildEnforcementTypes(SelectEnforcementType.WARRANT))
                    .warrantDetails(WarrantDetails.builder()
                            .anyRiskToBailiff(YesNoNotSure.YES)
                            .riskDetails(RiskDetails.builder()
                                    .violentDetails("Violent")
                                    .verbalThreatsDetails("Verbal")
                                    .firearmsDetails("Firearms")
                                    .build())
                            .build())
                    .build();

            RiskProfileEntity result = mapper.toEntity(orderEntity, order);

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
                    .chooseEnforcementType(buildEnforcementTypes(SelectEnforcementType.WARRANT))
                    .warrantDetails(WarrantDetails.builder().build())
                    .rawWarrantDetails(RawWarrantDetails.builder()
                            .vulnerablePeoplePresent(YesNoNotSure.YES)
                            .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                                    .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                                    .vulnerableReasonText("Vulnerability reason")
                                    .build())
                            .build())
                    .build();

            RiskProfileEntity result = mapper.toEntity(orderEntity, order);

            assertThat(result.getVulnerablePeoplePresent()).isEqualTo(YesNoNotSure.YES);
            assertThat(result.getVulnerableCategory()).isEqualTo(VulnerableCategory.VULNERABLE_ADULTS);
            assertThat(result.getVulnerableReasonText()).isEqualTo("Vulnerability reason");
        }

        @Test
        @DisplayName("maps warrant details when risk details is null")
        void mapsWarrantDetailsWhenRiskDetailsNull() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .chooseEnforcementType(buildEnforcementTypes(SelectEnforcementType.WARRANT))
                    .warrantDetails(WarrantDetails.builder()
                            .anyRiskToBailiff(YesNoNotSure.NO)
                            .build())
                    .rawWarrantDetails(RawWarrantDetails.builder()
                            .build())
                    .build();

            RiskProfileEntity result = mapper.toEntity(orderEntity, order);
            assertThat(result.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
            assertThat(result.getViolentDetails()).isNull();
            assertThat(result.getVerbalThreatsDetails()).isNull();
        }
    }

    @Nested
    @DisplayName("warrantOfRestitution")
    class WarrantOfRestitution {
        @Test
        @DisplayName("maps full warrant of restitution and risk details to entity")
        void mapsFullWarrantOfRestitutionAndRiskDetails() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .chooseEnforcementType(buildEnforcementTypes(SelectEnforcementType.WARRANT_OF_RESTITUTION))
                    .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                            .anyRiskToBailiff(YesNoNotSure.YES)
                            .riskDetails(RiskDetails.builder()
                                    .violentDetails("Violent")
                                    .verbalThreatsDetails("Verbal")
                                    .firearmsDetails("Firearms")
                                    .build())
                            .build())
                    .build();

            RiskProfileEntity result = mapper.toEntity(orderEntity, order);

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
                    .chooseEnforcementType(buildEnforcementTypes(SelectEnforcementType.WARRANT_OF_RESTITUTION))
                    .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder().build())
                    .rawWarrantRestDetails(RawWarrantRestDetails.builder()
                            .vulnerablePeoplePresentWarrantRest(YesNoNotSure.YES)
                            .vulnerableAdultsChildrenWarrantRest(VulnerableAdultsChildren.builder()
                                    .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                                    .vulnerableReasonText("Vulnerability reason")
                                    .build())
                            .build())
                    .build();

            RiskProfileEntity result = mapper.toEntity(orderEntity, order);

            assertThat(result.getVulnerablePeoplePresent()).isEqualTo(YesNoNotSure.YES);
            assertThat(result.getVulnerableCategory()).isEqualTo(VulnerableCategory.VULNERABLE_ADULTS);
            assertThat(result.getVulnerableReasonText()).isEqualTo("Vulnerability reason");
        }

        @Test
        @DisplayName("maps warrant of restitution details when risk details is null")
        void mapsWarrantOfRestitutionDetailsWhenRiskDetailsNull() {
            EnforcementOrderEntity orderEntity = new EnforcementOrderEntity();
            orderEntity.setId(UUID.randomUUID());
            EnforcementOrder order = EnforcementOrder.builder()
                    .chooseEnforcementType(buildEnforcementTypes(SelectEnforcementType.WARRANT_OF_RESTITUTION))
                    .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                            .anyRiskToBailiff(YesNoNotSure.NO)
                            .build())
                    .rawWarrantRestDetails(RawWarrantRestDetails.builder()
                            .build())
                    .build();

            RiskProfileEntity result = mapper.toEntity(orderEntity, order);
            assertThat(result.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
            assertThat(result.getViolentDetails()).isNull();
            assertThat(result.getVerbalThreatsDetails()).isNull();
        }
    }
}
