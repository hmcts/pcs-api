package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementSelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementSelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private EnforcementSelectedDefendantRepository enforcementSelectedDefendantRepository;

    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    @Mock
    private PartyRepository partyRepository;


    @Captor
    private ArgumentCaptor<EnforcementOrderEntity> enforcementOrderEntityCaptor;

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

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        EnforcementOrderEntity savedEntity = enforcementOrderEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
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
    void shouldAddSelectedDefendantsWhenProvided() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        String jessMayID = "6cd0fed0-6e90-4116-add4-5513f10c684f";
        String jamesMayID = "e29108f4-bb65-4b81-88d9-f319048fa8f0";

        List<DynamicStringListElement> selected = List.of(
            new DynamicStringListElement(jessMayID, "Jess May")
        );

        List<DynamicStringListElement> listItems = List.of(
            new DynamicStringListElement(jessMayID, "James May"),
            new DynamicStringListElement(jamesMayID, "Jess May")
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

        when(partyRepository.findAllById(anyList()))
            .thenReturn(List.of(partyJessMay));

        ArgumentCaptor<EnforcementSelectedDefendantEntity> captor =
            ArgumentCaptor.forClass(EnforcementSelectedDefendantEntity.class);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(draftCaseDataService)
            .deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder);

        verify(enforcementSelectedDefendantRepository, times(1)).save(captor.capture());

        EnforcementSelectedDefendantEntity savedEntity = captor.getValue();

        assertThat(savedEntity.getEnforcementCase().getClaim().getPcsCase().getId()).isEqualTo(pcsCaseId);
        assertThat(savedEntity.getParty().getId()).isEqualTo(UUID.fromString(jessMayID));
        assertThat(savedEntity.getParty().getFirstName()).isEqualTo("Jess");
        assertThat(savedEntity.getParty().getLastName()).isEqualTo("May");
    }

    @Test
    void shouldAddMultipleSelectedDefendantsWhenProvided() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        String jessMayID = "6cd0fed0-6e90-4116-add4-5513f10c684f";
        String jamesMayID = "e29108f4-bb65-4b81-88d9-f319048fa8f0";


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

        when(partyRepository.findAllById(anyList()))
            .thenReturn(List.of(partyJessMay, partyJamesMay));

        ArgumentCaptor<EnforcementSelectedDefendantEntity> captor =
            ArgumentCaptor.forClass(EnforcementSelectedDefendantEntity.class);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementSelectedDefendantRepository, times(2)).save(captor.capture());
        List<EnforcementSelectedDefendantEntity> savedEntities = captor.getAllValues();

        assertThat(savedEntities).extracting("party")
            .containsExactlyInAnyOrder(partyJessMay, partyJamesMay);

    }

    @Test
    void shouldNotAddAnySelectedDefendantsWhenNoneSelected() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        String jessMayID = "6cd0fed0-6e90-4116-add4-5513f10c684f";
        String jamesMayID = "e29108f4-bb65-4b81-88d9-f319048fa8f0";

        List<DynamicStringListElement> selected = emptyList();

        List<DynamicStringListElement> listItems = List.of(
            new DynamicStringListElement(jessMayID, "Jess May"),
            new DynamicStringListElement(jamesMayID, "James May")
        );

        final EnforcementOrder enforcementOrder =
            EnforcementDataUtil.buildEnforcementOrderWithSelectedDefendants(selected, listItems);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verifyNoInteractions(enforcementSelectedDefendantRepository);
        verify(draftCaseDataService)
            .deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder);
    }
}
