package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.RiskDetailsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.SelectedDefendantsMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantDetailsMapper;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory.VULNERABLE_CHILDREN;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementTypes;

@ExtendWith(MockitoExtension.class)
class WarrantStrategyTest {

    @Mock
    private RiskProfileRepository riskProfileRepository;
    @Mock
    private SelectedDefendantsMapper selectedDefendantsMapper;
    @Mock
    private SelectedDefendantRepository selectedDefendantRepository;
    @Mock
    private RiskDetailsMapper riskProfileMapper;
    @Mock
    private WarrantDetailsMapper warrantDetailsMapper;
    @Mock
    private WarrantRepository warrantRepository;
    @Captor
    private ArgumentCaptor<RiskProfileEntity> riskProfileCaptor;
    @Captor
    private ArgumentCaptor<WarrantEntity> warrantEntityCaptor;
    @Captor
    private ArgumentCaptor<List<SelectedDefendantEntity>> selectedDefendantEntityCaptor;

    @InjectMocks
    private WarrantStrategy underTest;

    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;
    private WarrantDetails warrantDetails;
    private RiskDetails riskDetails;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        riskDetails = RiskDetails.builder()
            .violentDetails("Violent behavior reported")
            .firearmsDetails("Firearms present")
            .criminalDetails("Criminal history")
            .verbalThreatsDetails("Verbal threats made")
            .protestGroupDetails("Member of protest group")
            .policeSocialServicesDetails("Police involvement")
            .animalsDetails("Aggressive dogs on premises")
            .build();

        warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).riskDetails(riskDetails).build();

        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
            .vulnerableCategory(VULNERABLE_CHILDREN).vulnerableReasonText("Young children present").build();

        RawWarrantDetails rawWarrantDetails = RawWarrantDetails.builder()
            .vulnerablePeoplePresent(YesNoNotSure.YES).vulnerableAdultsChildren(vulnerableAdultsChildren).build();

        enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails)
            .rawWarrantDetails(rawWarrantDetails).build();
    }

    @Test
    void shouldProcessWarrantDetailsAndSaveToRepository() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();
        savedWarrantEntity.setId(UUID.randomUUID());

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(warrantRepository.save(warrantEntity)).thenReturn(savedWarrantEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantDetailsMapper).toEntity(enforcementOrder, enforcementOrderEntity);
        verify(warrantRepository).save(warrantEntity);
        verify(enforcementOrderEntity).setWarrantDetails(savedWarrantEntity);
    }

    @Test
    void shouldSetSavedWarrantDetailsOnEnforcementOrderEntity() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();
        UUID warrantId = UUID.randomUUID();
        savedWarrantEntity.setId(warrantId);

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(warrantRepository.save(warrantEntity)).thenReturn(savedWarrantEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantRepository).save(warrantEntityCaptor.capture());
        WarrantEntity capturedEntity = warrantEntityCaptor.getValue();
        assertThat(capturedEntity).isEqualTo(warrantEntity);
        verify(enforcementOrderEntity).setWarrantDetails(savedWarrantEntity);
    }

    @Test
    void shouldProcessWarrantBeforeRiskProfile() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(warrantRepository.save(warrantEntity)).thenReturn(savedWarrantEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        InOrder inOrder = inOrder(warrantRepository, riskProfileRepository);
        inOrder.verify(warrantRepository).save(warrantEntity);
        inOrder.verify(riskProfileRepository).save(any(RiskProfileEntity.class));
    }

    @Test
    void shouldCallWarrantDetailsMapper() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(warrantRepository.save(warrantEntity)).thenReturn(savedWarrantEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantDetailsMapper).toEntity(
            argThat(order -> order.equals(enforcementOrder)),
            argThat(entity -> entity.equals(enforcementOrderEntity))
        );
    }

    @Test
    void shouldProcessAllThreeStepsInCorrectOrder() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();
        List<SelectedDefendantEntity> defendants = List.of(new SelectedDefendantEntity());

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(warrantRepository.save(warrantEntity)).thenReturn(savedWarrantEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        InOrder inOrder = inOrder(
            warrantRepository,
            riskProfileRepository,
            selectedDefendantRepository
        );
        inOrder.verify(warrantRepository).save(warrantEntity);
        inOrder.verify(riskProfileRepository).save(any(RiskProfileEntity.class));
        inOrder.verify(selectedDefendantRepository).saveAll(defendants);
    }

    @Test
    void shouldProcessWarrantDetailsSuccessfully() {
        // Given
        RiskProfileEntity savedEntity = createExpectedRiskProfileEntity();
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(savedEntity);
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
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

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

    @ParameterizedTest
    @MethodSource("provideSelectedDefendantScenarios")
    void shouldHandleSelectedDefendantsBasedOnMapperResult(List<SelectedDefendantEntity> defendants,
                                                           boolean shouldCallSaveAll) {
        // Given
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        if (shouldCallSaveAll) {
            verify(selectedDefendantRepository).saveAll(defendants);
        } else {
            verify(selectedDefendantRepository, never()).saveAll(any());
        }
    }

    private static Stream<Arguments> provideSelectedDefendantScenarios() {
        SelectedDefendantEntity defendant1 = new SelectedDefendantEntity();
        SelectedDefendantEntity defendant2 = new SelectedDefendantEntity();

        return Stream.of(
            Arguments.of(List.of(defendant1, defendant2), true),
            Arguments.of(Collections.emptyList(), false),
            Arguments.of(null, false)
        );
    }


    @Test
    void shouldHandleWarrantDetailsWithNoRiskToBailiff() {
        // Given
        warrantDetails.setAnyRiskToBailiff(YesNoNotSure.NO);
        RiskProfileEntity riskProfileEntity = new RiskProfileEntity();
        riskProfileEntity.setAnyRiskToBailiff(YesNoNotSure.NO);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
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
        RiskProfileEntity riskProfileEntity = createExpectedRiskProfileEntity();
        riskProfileEntity.setAnyRiskToBailiff(null);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(riskProfileEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(Collections.emptyList());
        EnforcementOrder minimalOrder = EnforcementOrder.builder().build();

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
        RiskProfileEntity riskProfileEntity = createExpectedRiskProfileEntity();
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);
        when(riskProfileRepository.save(riskProfileEntity)).thenReturn(riskProfileEntity);
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

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
        List<SelectedDefendantEntity> defendants = List.of(new SelectedDefendantEntity());
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(new RiskProfileEntity());
        when(riskProfileRepository.save(any(RiskProfileEntity.class))).thenReturn(new RiskProfileEntity());
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        InOrder inOrder = inOrder(riskProfileRepository, selectedDefendantRepository);
        inOrder.verify(riskProfileRepository).save(any(RiskProfileEntity.class));
        inOrder.verify(selectedDefendantRepository).saveAll(defendants);
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
        underTest.process(enforcementOrderEntity, enfOrder);

        // Then: service calls mapper and saves returned risk profile
        verify(riskProfileMapper).toEntity(any(), any());
        verify(riskProfileRepository).save(riskProfileCaptor.capture());
    }

    @Test
    void shouldAddMultipleSelectedDefendantsWhenProvided() {
        // Given
        String jessMayID = UUID.randomUUID().toString();
        String jamesMayID = UUID.randomUUID().toString();

        List<DynamicStringListElement> selected = List.of(
            new DynamicStringListElement(jessMayID, "Jess May"),
            new DynamicStringListElement(jamesMayID, "James May")
        );

        List<DynamicStringListElement> listItems = List.of(
            new DynamicStringListElement(jessMayID, "Jess May"),
            new DynamicStringListElement(jamesMayID, "James May")
        );

        final EnforcementOrder enfOrder =
            EnforcementDataUtil.buildEnforcementOrderWithSelectedDefendants(selected, listItems);

        PartyEntity partyJessMay = PartyEntity.builder()
            .id(UUID.fromString(jessMayID))
            .firstName("Jess")
            .lastName("May")
            .build();

        PartyEntity partyJamesMay = PartyEntity.builder()
            .id(UUID.fromString(jamesMayID))
            .firstName("James")
            .lastName("May")
            .build();

        SelectedDefendantEntity entityJess = new SelectedDefendantEntity();
        entityJess.setParty(partyJessMay);

        SelectedDefendantEntity entityJames = new SelectedDefendantEntity();
        entityJames.setParty(partyJamesMay);

        when(selectedDefendantsMapper.mapToEntities(any(EnforcementOrderEntity.class)))
            .thenReturn(List.of(entityJess, entityJames));

        // When
        underTest.process(enforcementOrderEntity, enfOrder);

        // Then
        verify(selectedDefendantRepository)
            .saveAll(selectedDefendantEntityCaptor.capture());
        List<SelectedDefendantEntity> savedEntities = selectedDefendantEntityCaptor.getValue();

        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities)
            .extracting(SelectedDefendantEntity::getParty)
            .containsExactlyInAnyOrder(partyJessMay, partyJamesMay);
    }

    @Test
    void shouldNotAddAnySelectedDefendantsWhenEvictEveryoneIsYes() {
        // Given
        final EnforcementOrder enfOrder = EnforcementOrder.builder()
                .chooseEnforcementType(buildEnforcementTypes(WARRANT))
            .warrantDetails(WarrantDetails.builder()
                                .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                                               .correctNameAndAddress(SimpleYesNo.YES)
                                                               .build())
                                .peopleToEvict(PeopleToEvict.builder()
                                                   .evictEveryone(SimpleYesNo.YES)
                                                   .build())
                                .build())
            .rawWarrantDetails(RawWarrantDetails.builder().selectedDefendants(null).build())
            .build();

        // When
        underTest.process(enforcementOrderEntity, enfOrder);

        // Then
        verifyNoInteractions(selectedDefendantRepository);
    }

    private RiskProfileEntity createExpectedRiskProfileEntity() {
        RiskProfileEntity entity = new RiskProfileEntity();
        entity.setEnforcementOrder(enforcementOrderEntity);
        entity.setAnyRiskToBailiff(YesNoNotSure.YES);
        entity.setViolentDetails("Violent behavior reported");
        entity.setFirearmsDetails("Firearms present");
        entity.setCriminalDetails("Criminal history");
        entity.setVerbalThreatsDetails("Verbal threats made");
        entity.setProtestGroupDetails("Member of protest group");
        entity.setPoliceSocialServicesDetails("Police involvement");
        entity.setAnimalsDetails("Aggressive dogs on premises");
        entity.setVulnerablePeoplePresent(YesNoNotSure.YES);
        entity.setVulnerableCategory(VULNERABLE_CHILDREN);
        entity.setVulnerableReasonText("Young children present");
        return entity;
    }

}
