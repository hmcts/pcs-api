package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.RiskDetailsMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory.VULNERABLE_CHILDREN;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.createExpectedRiskProfileEntity;

@ExtendWith(MockitoExtension.class)
class RiskProfileServiceTest {

    @Mock
    private RiskDetailsMapper riskProfileMapper;
    @Mock
    private RiskProfileRepository riskProfileRepository;
    @InjectMocks
    private RiskProfileService underTest;

    private RiskDetails riskDetails;
    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;
    private WarrantDetails warrantDetails;
    @Captor
    private ArgumentCaptor<RiskProfileEntity> riskProfileCaptor;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).riskDetails(riskDetails).build();

        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerableCategory(VULNERABLE_CHILDREN).vulnerableReasonText("Young children present").build();

        RawWarrantDetails rawWarrantDetails = RawWarrantDetails.builder()
                .vulnerablePeoplePresent(YesNoNotSure.YES).vulnerableAdultsChildren(vulnerableAdultsChildren).build();

        enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails)
                .rawWarrantDetails(rawWarrantDetails).build();

        riskDetails = RiskDetails.builder()
                .violentDetails("Violent behavior reported")
                .firearmsDetails("Firearms present")
                .criminalDetails("Criminal history")
                .verbalThreatsDetails("Verbal threats made")
                .protestGroupDetails("Member of protest group")
                .policeSocialServicesDetails("Police involvement")
                .animalsDetails("Aggressive dogs on premises")
                .build();
    }

    @ParameterizedTest
    @MethodSource("provideNullWarrantDetailsScenarios")
    void shouldHandleNullValuesInWarrantDetails(
            RawWarrantDetails rawWarrantDetails,
            YesNoNotSure expectedVulnerablePeoplePresent,
            boolean expectNullVulnerableFields
    ) {
        // Given
        enforcementOrder.setRawWarrantDetails(rawWarrantDetails);
        RiskProfileEntity riskProfileEntity = new RiskProfileEntity();
        if (expectedVulnerablePeoplePresent != null) {
            riskProfileEntity.setVulnerablePeoplePresent(expectedVulnerablePeoplePresent);
        }
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getVulnerablePeoplePresent()).isEqualTo(expectedVulnerablePeoplePresent);
        if (expectNullVulnerableFields) {
            assertThat(capturedEntity.getVulnerableCategory()).isNull();
            assertThat(capturedEntity.getVulnerableReasonText()).isNull();
        }
    }

    private static Stream<Arguments> provideNullWarrantDetailsScenarios() {
        RawWarrantDetails withNullVulnerableChildren = RawWarrantDetails.builder()
                .vulnerablePeoplePresent(YesNoNotSure.YES)
                .vulnerableAdultsChildren(null)
                .build();

        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(withNullVulnerableChildren, YesNoNotSure.YES, true)
        );
    }

    @Test
    void shouldProcessRiskDetailsSuccessfully() {
        // Given
        RiskProfileEntity savedEntity = createExpectedRiskProfileEntity(enforcementOrderEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(savedEntity);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(savedEntity);

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(capturedEntity.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.YES);
        assertThat(capturedEntity.getViolentDetails()).isEqualTo("Violent behavior reported");
        assertThat(capturedEntity.getFirearmsDetails()).isEqualTo("Firearms present");
        assertThat(capturedEntity.getCriminalDetails()).isEqualTo("Criminal history");
        assertThat(capturedEntity.getVerbalThreatsDetails()).isEqualTo("Verbal threats made");
        assertThat(capturedEntity.getProtestGroupDetails()).isEqualTo("Member of protest group");
        assertThat(capturedEntity.getPoliceSocialServicesDetails()).isEqualTo("Police involvement");
        assertThat(capturedEntity.getAnimalsDetails()).isEqualTo("Aggressive dogs on premises");
        assertThat(capturedEntity.getVulnerablePeoplePresent()).isEqualTo(YesNoNotSure.YES);
        assertThat(capturedEntity.getVulnerableCategory()).isEqualTo(VULNERABLE_CHILDREN);
        assertThat(capturedEntity.getVulnerableReasonText()).isEqualTo("Young children present");
    }

    @Test
    void shouldHandleWarrantDetailsWithNoRiskToBailiff() {
        // Given
        warrantDetails.setAnyRiskToBailiff(YesNoNotSure.NO);
        RiskProfileEntity riskProfileEntity = new RiskProfileEntity();
        riskProfileEntity.setAnyRiskToBailiff(YesNoNotSure.NO);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldHandleMinimalEnforcementOrder() {
        // Given
        RiskProfileEntity riskProfileEntity = createExpectedRiskProfileEntity(enforcementOrderEntity);
        riskProfileEntity.setAnyRiskToBailiff(null);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(riskProfileEntity);
        EnforcementOrder minimalOrder = EnforcementOrder.builder().build();

        // When
        underTest.processRisk(minimalOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(capturedEntity.getAnyRiskToBailiff()).isNull();
    }

    @Test
    void shouldMapAllRiskDetailsFieldsCorrectly() {
        // Given
        RiskProfileEntity riskProfileEntity = createExpectedRiskProfileEntity(enforcementOrderEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
        when(riskProfileRepository.save(riskProfileEntity)).thenReturn(riskProfileEntity);

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getViolentDetails()).isEqualTo(riskDetails.getViolentDetails());
        assertThat(capturedEntity.getFirearmsDetails()).isEqualTo(riskDetails.getFirearmsDetails());
        assertThat(capturedEntity.getCriminalDetails()).isEqualTo(riskDetails.getCriminalDetails());
        assertThat(capturedEntity.getVerbalThreatsDetails()).isEqualTo(riskDetails.getVerbalThreatsDetails());
        assertThat(capturedEntity.getProtestGroupDetails()).isEqualTo(riskDetails.getProtestGroupDetails());
        assertThat(capturedEntity.getPoliceSocialServicesDetails())
                .isEqualTo(riskDetails.getPoliceSocialServicesDetails());
        assertThat(capturedEntity.getAnimalsDetails()).isEqualTo(riskDetails.getAnimalsDetails());
    }

    @Test
    void shouldSaveRiskProfileBeforeSelectedDefendants() {
        // Given
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(any(RiskProfileEntity.class));
    }

    @Test
    void shouldPersistVulnerabilityDetailsInRiskProfile() {
        // Given: order with raw warrant details (vulnerability)
        final EnforcementOrder enfOrder = EnforcementDataUtil.buildEnforcementOrderWithVulnerability();
        final RiskProfileEntity stubbedRiskProfile = new RiskProfileEntity();
        stubbedRiskProfile.setVulnerablePeoplePresent(YesNoNotSure.YES);
        stubbedRiskProfile.setVulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS);
        stubbedRiskProfile.setVulnerableReasonText("Vulnerability reason");
        when(riskProfileMapper.toEntity(any(EnforcementOrderEntity.class), eq(enfOrder)))
                .thenReturn(stubbedRiskProfile);

        // When
        underTest.processRisk(enfOrder, enforcementOrderEntity);

        // Then: service calls mapper and saves returned risk profile
        verify(riskProfileMapper).toEntity(any(), any());
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
    }
}