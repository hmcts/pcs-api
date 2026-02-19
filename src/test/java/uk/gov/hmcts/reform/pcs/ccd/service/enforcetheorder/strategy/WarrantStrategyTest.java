package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.SelectedDefendantsMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory.VULNERABLE_CHILDREN;

@ExtendWith(MockitoExtension.class)
class WarrantStrategyTest {

    @Mock
    private RiskProfileRepository riskProfileRepository;
    @Mock
    private SelectedDefendantsMapper selectedDefendantsMapper;
    @Mock
    private SelectedDefendantRepository selectedDefendantRepository;

    @InjectMocks
    private WarrantStrategy underTest;

    @Captor
    private ArgumentCaptor<RiskProfileEntity> riskProfileCaptor;

    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;
    private WarrantDetails warrantDetails;
    private RawWarrantDetails rawWarrantDetails;
    private EnforcementRiskDetails riskDetails;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        riskDetails = EnforcementRiskDetails.builder()
            .enforcementViolentDetails("Violent behavior reported")
            .enforcementFirearmsDetails("Firearms present")
            .enforcementCriminalDetails("Criminal history")
            .enforcementVerbalOrWrittenThreatsDetails("Verbal threats made")
            .enforcementProtestGroupMemberDetails("Member of protest group")
            .enforcementPoliceOrSocialServicesDetails("Police involvement")
            .enforcementDogsOrOtherAnimalsDetails("Aggressive dogs on premises")
            .build();

        warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).riskDetails(riskDetails).build();

        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
            .vulnerableCategory(VULNERABLE_CHILDREN).vulnerableReasonText("Young children present").build();

        rawWarrantDetails = RawWarrantDetails.builder()
            .vulnerablePeoplePresent(YesNoNotSure.YES).vulnerableAdultsChildren(vulnerableAdultsChildren).build();

        enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails)
            .rawWarrantDetails(rawWarrantDetails).build();
    }

    @Test
    void shouldProcessWarrantDetailsSuccessfully() {
        // Given
        RiskProfileEntity savedEntity = new RiskProfileEntity();
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(savedEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

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
    void shouldHandleNullWarrantDetails() {
        // Given
        enforcementOrder.setWarrantDetails(null);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(capturedEntity.getAnyRiskToBailiff()).isNull();
        assertThat(capturedEntity.getViolentDetails()).isNull();
        assertThat(capturedEntity.getFirearmsDetails()).isNull();
    }

    @Test
    void shouldHandleNullRiskDetails() {
        // Given
        warrantDetails.setRiskDetails(null);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(capturedEntity.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.YES);
        assertThat(capturedEntity.getViolentDetails()).isNull();
        assertThat(capturedEntity.getFirearmsDetails()).isNull();
    }

    @Test
    void shouldHandleNullRawWarrantDetails() {
        // Given
        enforcementOrder.setRawWarrantDetails(null);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getVulnerablePeoplePresent()).isNull();
        assertThat(capturedEntity.getVulnerableCategory()).isNull();
        assertThat(capturedEntity.getVulnerableReasonText()).isNull();
    }

    @Test
    void shouldHandleNullVulnerableAdultsChildren() {
        // Given
        rawWarrantDetails.setVulnerableAdultsChildren(null);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getVulnerablePeoplePresent()).isEqualTo(YesNoNotSure.YES);
        assertThat(capturedEntity.getVulnerableCategory()).isNull();
        assertThat(capturedEntity.getVulnerableReasonText()).isNull();
    }

    @Test
    void shouldSaveSelectedDefendantsWhenPresent() {
        // Given
        SelectedDefendantEntity defendant1 = new SelectedDefendantEntity();
        SelectedDefendantEntity defendant2 = new SelectedDefendantEntity();
        List<SelectedDefendantEntity> defendants = List.of(defendant1, defendant2);

        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(selectedDefendantRepository).saveAll(defendants);
    }

    @Test
    void shouldNotSaveSelectedDefendantsWhenEmpty() {
        // Given
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(selectedDefendantRepository, never()).saveAll(any());
    }

    @Test
    void shouldNotSaveSelectedDefendantsWhenNull() {
        // Given
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(null);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(selectedDefendantRepository, never()).saveAll(any());
    }

    @Test
    void shouldHandleWarrantDetailsWithNoRiskToBailiff() {
        // Given
        warrantDetails.setAnyRiskToBailiff(YesNoNotSure.NO);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getAnyRiskToBailiff()).isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldHandleMinimalEnforcementOrder() {
        // Given
        EnforcementOrder minimalOrder = EnforcementOrder.builder().build();
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, minimalOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(capturedEntity.getAnyRiskToBailiff()).isNull();
    }

    @Test
    void shouldMapAllRiskDetailsFieldsCorrectly() {
        // Given
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
        RiskProfileEntity capturedEntity = riskProfileCaptor.getValue();

        assertThat(capturedEntity.getViolentDetails()).isEqualTo(riskDetails.getEnforcementViolentDetails());
        assertThat(capturedEntity.getFirearmsDetails()).isEqualTo(riskDetails.getEnforcementFirearmsDetails());
        assertThat(capturedEntity.getCriminalDetails()).isEqualTo(riskDetails.getEnforcementCriminalDetails());
        assertThat(capturedEntity.getVerbalThreatsDetails())
            .isEqualTo(riskDetails.getEnforcementVerbalOrWrittenThreatsDetails());
        assertThat(capturedEntity.getProtestGroupDetails())
            .isEqualTo(riskDetails.getEnforcementProtestGroupMemberDetails());
        assertThat(capturedEntity.getPoliceSocialServicesDetails())
            .isEqualTo(riskDetails.getEnforcementPoliceOrSocialServicesDetails());
        assertThat(capturedEntity.getAnimalsDetails())
            .isEqualTo(riskDetails.getEnforcementDogsOrOtherAnimalsDetails());
    }

    @Test
    void shouldSaveRiskProfileBeforeSelectedDefendants() {
        // Given
        List<SelectedDefendantEntity> defendants = List.of(new SelectedDefendantEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        InOrder inOrder = inOrder(riskProfileRepository, selectedDefendantRepository);
        inOrder.verify(selectedDefendantRepository).saveAll(defendants);
        inOrder.verify(riskProfileRepository).save(any(RiskProfileEntity.class));
    }
}
