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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
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
    private RiskProfileService riskProfileService;
    @Mock
    private SelectedDefendantsMapper selectedDefendantsMapper;
    @Mock
    private SelectedDefendantRepository selectedDefendantRepository;

    @Mock
    private WarrantDetailsMapper warrantDetailsMapper;
    @Mock
    private WarrantRepository warrantRepository;
    @Captor
    private ArgumentCaptor<WarrantEntity> warrantEntityCaptor;
    @Captor
    private ArgumentCaptor<List<SelectedDefendantEntity>> selectedDefendantEntityCaptor;

    @InjectMocks
    private WarrantStrategy underTest;

    private EnforcementOrderEntity enforcementOrderEntity;
    private EnforcementOrder enforcementOrder;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        WarrantDetails warrantDetails = WarrantDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).build();

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
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity))
            .thenReturn(Collections.emptyList());
        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(warrantDetailsMapper).toEntity(enforcementOrder, enforcementOrderEntity);
        verify(warrantRepository).save(warrantEntity);
        verify(enforcementOrderEntity).setWarrantDetails(savedWarrantEntity);
        verify(riskProfileService).processRisk(enforcementOrder, enforcementOrderEntity);
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
    void shouldCallWarrantDetailsMapper() {
        // Given
        WarrantEntity warrantEntity = new WarrantEntity();
        WarrantEntity savedWarrantEntity = new WarrantEntity();

        when(warrantDetailsMapper.toEntity(enforcementOrder, enforcementOrderEntity))
            .thenReturn(warrantEntity);
        when(warrantRepository.save(warrantEntity)).thenReturn(savedWarrantEntity);
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
        when(selectedDefendantsMapper.mapToEntities(enforcementOrderEntity)).thenReturn(defendants);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        InOrder inOrder = inOrder(
            warrantRepository,
            selectedDefendantRepository
        );
        inOrder.verify(warrantRepository).save(warrantEntity);
        inOrder.verify(selectedDefendantRepository).saveAll(defendants);
    }

    @ParameterizedTest
    @MethodSource("provideSelectedDefendantScenarios")
    void shouldHandleSelectedDefendantsBasedOnMapperResult(List<SelectedDefendantEntity> defendants,
                                                           boolean shouldCallSaveAll) {
        // Given
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
                                                               .correctNameAndAddress(VerticalYesNo.YES)
                                                               .build())
                                .peopleToEvict(PeopleToEvict.builder()
                                                   .evictEveryone(VerticalYesNo.YES)
                                                   .build())
                                .build())
            .rawWarrantDetails(RawWarrantDetails.builder().selectedDefendants(null).build())
            .build();

        // When
        underTest.process(enforcementOrderEntity, enfOrder);

        // Then
        verifyNoInteractions(selectedDefendantRepository);
    }
}
