package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementSelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrantofrestitution.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementRiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementSelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrantofrestitution.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementRiskProfileMapper;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderServiceTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    @Mock
    private EnforcementRiskProfileRepository enforcementRiskProfileRepository;

    @Mock
    private EnforcementRiskProfileMapper enforcementRiskProfileMapper;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private EnforcementSelectedDefendantRepository enforcementSelectedDefendantRepository;
    @Mock
    private WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    @Captor
    private ArgumentCaptor<List<EnforcementSelectedDefendantEntity>> captor;

    @Captor
    private ArgumentCaptor<EnforcementOrderEntity> enforcementOrderEntityCaptor;

    @Captor
    private ArgumentCaptor<EnforcementRiskProfileEntity> enforcementRiskProfileEntityCaptor;

    @Captor
    private ArgumentCaptor<WarrantOfRestitutionEntity> warrantOfRestitutionEntityCaptor;

    @Mock
    private SelectedDefendantsMapper selectedDefendantsMapper;

    private final UUID enforcementOrderId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");

    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;

    @Test
    void shouldThrowExceptionWhenNoEnforcementOrder() {
        // Given
        when(enforcementOrderRepository.findById(enforcementOrderId)).thenReturn(Optional.empty());

        // When &
        // Then
        assertThatThrownBy(() -> enforcementOrderService.loadEnforcementOrder(enforcementOrderId))
                .isInstanceOf(EnforcementOrderNotFoundException.class)
                .hasMessageContaining("No enforcement order found for case reference");
    }

    @Test
    void shouldReturnEnforcementOrderWhenFound() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final ClaimEntity claimEntity = pcsCaseEntity.getClaims().getFirst();
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        final EnforcementOrderEntity enforcementOrderEntity =
                EnforcementDataUtil.buildEnforcementOrderEntity(enforcementOrderId, claimEntity, enforcementOrder);
        when(enforcementOrderRepository.findById(enforcementOrderId)).thenReturn(Optional.of(enforcementOrderEntity));

        // When
        EnforcementOrderEntity retrievedEnforcementOrderEntity =
                enforcementOrderService.loadEnforcementOrder(enforcementOrderId);

        // Then
        assertThat(retrievedEnforcementOrderEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        assertThat(retrievedEnforcementOrderEntity.getId()).isEqualTo(enforcementOrderId);
        assertThat(retrievedEnforcementOrderEntity.getClaim().getId()).isEqualTo(claimId);
    }

    @Test
    void shouldSaveNewSubmittedEnforcementData() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        final EnforcementRiskProfileEntity stubbedRiskProfile = new EnforcementRiskProfileEntity();
        stubbedRiskProfile.setAnyRiskToBailiff(YesNoNotSure.YES);
        stubbedRiskProfile.setViolentDetails("Violent");
        stubbedRiskProfile.setVerbalThreatsDetails("Verbal");

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementRiskProfileMapper.toEntity(any(EnforcementOrderEntity.class), eq(enforcementOrder)))
                .thenReturn(stubbedRiskProfile);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        EnforcementOrderEntity savedEntity = enforcementOrderEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);

        verify(enforcementRiskProfileMapper).toEntity(savedEntity, enforcementOrder);
        verify(enforcementRiskProfileRepository).save(enforcementRiskProfileEntityCaptor.capture());
        assertThat(enforcementRiskProfileEntityCaptor.getValue()).isSameAs(stubbedRiskProfile);
    }

    @Test
    void shouldThrowExceptionWhenNoClaimFound() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        pcsCaseEntity.setClaims(null);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When &
        // Then
        assertThatThrownBy(() ->
                enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
                .isInstanceOf(ClaimNotFoundException.class)
                .hasMessageContaining("No claim found for case reference");
        verifyNoInteractions(draftCaseDataService);
        verifyNoInteractions(enforcementRiskProfileMapper);
        verifyNoInteractions(enforcementRiskProfileRepository);
    }

    @Test
    void shouldDeleteDraftEnforcementDataWhenSubmittedSuccessfully() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder);
    }

    @Test
    void shouldPersistRiskProfileWithMinimalDataWhenWarrantButNoWarrantOrRawDetails() {
        // Given: WARRANT type but no warrant or raw details
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .selectEnforcementType(SelectEnforcementType.WARRANT)
                .build();
        final EnforcementRiskProfileEntity stubbedRiskProfile = new EnforcementRiskProfileEntity();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementRiskProfileMapper.toEntity(any(EnforcementOrderEntity.class), eq(enforcementOrder)))
                .thenReturn(stubbedRiskProfile);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then: service calls mapper and saves returned risk profile
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        verify(enforcementRiskProfileMapper).toEntity(enforcementOrderEntityCaptor.getValue(), enforcementOrder);
        verify(enforcementRiskProfileRepository).save(enforcementRiskProfileEntityCaptor.capture());
        assertThat(enforcementRiskProfileEntityCaptor.getValue()).isSameAs(stubbedRiskProfile);
    }

    @Test
    void shouldNotPersistRiskProfileWhenEnforcementTypeIsWrit() {
        // Given: WRIT type (risk profile only applies to warrant)
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .selectEnforcementType(SelectEnforcementType.WRIT)
                .build();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        verifyNoInteractions(enforcementRiskProfileMapper);
        verifyNoInteractions(enforcementRiskProfileRepository);
    }

    @Test
    void shouldNotPersistRiskProfileWhenEnforcementTypeIsNull() {
        // Given: no enforcement type set
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        verifyNoInteractions(enforcementRiskProfileMapper);
        verifyNoInteractions(enforcementRiskProfileRepository);
    }

    @Test
    void shouldPersistVulnerabilityDetailsInRiskProfile() {
        // Given: order with raw warrant details (vulnerability)
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrderWithVulnerability();
        final EnforcementRiskProfileEntity stubbedRiskProfile = new EnforcementRiskProfileEntity();
        stubbedRiskProfile.setVulnerablePeoplePresent(YesNoNotSure.YES);
        stubbedRiskProfile.setVulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS);
        stubbedRiskProfile.setVulnerableReasonText("Vulnerability reason");

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementRiskProfileMapper.toEntity(any(EnforcementOrderEntity.class), eq(enforcementOrder)))
                .thenReturn(stubbedRiskProfile);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then: service calls mapper and saves returned risk profile
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        verify(enforcementRiskProfileMapper).toEntity(enforcementOrderEntityCaptor.getValue(), enforcementOrder);
        verify(enforcementRiskProfileRepository).save(enforcementRiskProfileEntityCaptor.capture());
        assertThat(enforcementRiskProfileEntityCaptor.getValue()).isSameAs(stubbedRiskProfile);
    }

    @Test
    void shouldAddSelectedDefendantsWhenProvided() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        String jessMayID = UUID.randomUUID().toString();

        List<DynamicStringListElement> selected = List.of(
            new DynamicStringListElement(jessMayID, "Jess May")
        );

        List<DynamicStringListElement> listItems = List.of(
            new DynamicStringListElement(jessMayID, "Jess May")
        );

        final EnforcementOrder enforcementOrder =
            EnforcementDataUtil.buildEnforcementOrderWithSelectedDefendants(selected, listItems);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(pcsCaseEntity));

        PartyEntity partyJessMay = PartyEntity.builder()
            .id(UUID.fromString(jessMayID))
            .firstName("Jess")
            .lastName("May")
            .build();

        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);

        EnforcementSelectedDefendantEntity entity = new EnforcementSelectedDefendantEntity();
        entity.setEnforcementCase(enforcementOrderEntity);
        entity.setParty(partyJessMay);

        when(selectedDefendantsMapper.mapToEntities(any(EnforcementOrderEntity.class)))
            .thenReturn(List.of(entity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(draftCaseDataService)
            .deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder);

        verify(enforcementSelectedDefendantRepository, times(1)).saveAll(captor.capture());
        List<EnforcementSelectedDefendantEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).hasSize(1);

        EnforcementSelectedDefendantEntity savedEntity = savedEntities.getFirst();

        assertThat(savedEntity.getParty().getId()).isEqualTo(UUID.fromString(jessMayID));
        assertThat(savedEntity.getParty().getFirstName()).isEqualTo("Jess");
        assertThat(savedEntity.getParty().getLastName()).isEqualTo("May");
    }

    @Test
    void shouldAddMultipleSelectedDefendantsWhenProvided() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
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

        final EnforcementOrder enforcementOrder =
            EnforcementDataUtil.buildEnforcementOrderWithSelectedDefendants(selected, listItems);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(pcsCaseEntity));

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

        EnforcementSelectedDefendantEntity entityJess = new EnforcementSelectedDefendantEntity();
        entityJess.setParty(partyJessMay);

        EnforcementSelectedDefendantEntity entityJames = new EnforcementSelectedDefendantEntity();
        entityJames.setParty(partyJamesMay);

        when(selectedDefendantsMapper.mapToEntities(any(EnforcementOrderEntity.class)))
            .thenReturn(List.of(entityJess, entityJames));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementSelectedDefendantRepository, times(1)).saveAll(captor.capture());
        List<EnforcementSelectedDefendantEntity> savedEntities = captor.getValue();

        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities)
            .extracting(EnforcementSelectedDefendantEntity::getParty)
            .containsExactlyInAnyOrder(partyJessMay, partyJamesMay);
    }


    @Test
    void shouldNotAddAnySelectedDefendantsWhenEvictEveryoneIsYes() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);

        final EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .selectEnforcementType(SelectEnforcementType.WARRANT)
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

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verifyNoInteractions(enforcementSelectedDefendantRepository);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder);
    }

    @Test
    void shouldPersistWarrantOfRestitutionWhenEnforcementOrderIsSaved() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        enforcementOrder.setSelectEnforcementType(SelectEnforcementType.WARRANT_OF_RESTITUTION);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(warrantOfRestitutionRepository).save(warrantOfRestitutionEntityCaptor.capture());
        WarrantOfRestitutionEntity savedEntity = warrantOfRestitutionEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder().getEnforcementOrder()).isEqualTo(enforcementOrder);
    }
}
