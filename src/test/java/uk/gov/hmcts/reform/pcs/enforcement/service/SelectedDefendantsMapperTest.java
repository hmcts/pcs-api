package uk.gov.hmcts.reform.pcs.enforcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildEnforcementOrderWithSelectedDefendants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.SelectedDefendantsMapper;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SelectedDefendantsMapperTest {

    @Mock
    private PartyRepository partyRepository;

    @InjectMocks
    private SelectedDefendantsMapper selectedDefendantsMapper;

    @Test
    void shouldMapSelectedDefendantsToEntities() {
        // Given
        UUID jessMayID = UUID.randomUUID();
        UUID jamesMayID = UUID.randomUUID();

        PartyEntity partyJessMay = PartyEntity.builder()
            .id(jessMayID)
            .firstName("Jess")
            .lastName("May")
            .build();

        PartyEntity partyJamesMay = PartyEntity.builder()
            .id(jamesMayID)
            .firstName("James")
            .lastName("May")
            .build();

        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        List<DynamicStringListElement> selected = List.of(
                new DynamicStringListElement(jessMayID.toString(), "Jess May"),
                new DynamicStringListElement(jamesMayID.toString(), "James May")
        );

        List<DynamicStringListElement> listItems = List.of(
                new DynamicStringListElement(jessMayID.toString(), "Jess May"),
                new DynamicStringListElement(jamesMayID.toString(), "James May")
        );

        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSelectedDefendants(selected, listItems);

        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);

        when(partyRepository.findAllById(List.of(jessMayID, jamesMayID)))
            .thenReturn(List.of(partyJessMay, partyJamesMay));

        // When

        List<SelectedDefendantEntity> entities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        // Then
        assertThat(entities).hasSize(2);

        assertThat(entities)
            .extracting(e -> e.getParty().getId())
            .containsExactlyInAnyOrder(jessMayID, jamesMayID);

        assertThat(entities)
            .allSatisfy(e -> assertThat(e.getEnfCase())
                .isEqualTo(enforcementOrderEntity));
    }

    @Test
    void shouldReturnEmptyListWhenNoSelectedDefendants() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .rawWarrantDetails(RawWarrantDetails.builder()
                                   .selectedDefendants(null)
                                   .build())
            .build();

        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);

        // When
        List<SelectedDefendantEntity> entities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        // Then
        assertThat(entities).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenTheDraftHasNoRawWarrantDetails() {
        // Given a resumed draft: only enforceTheOrder's start callback populates rawWarrantDetails
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setEnforcementOrder(EnforcementOrder.builder().build());

        // When
        List<SelectedDefendantEntity> entities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        // Then
        assertThat(entities).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenSelectedDefendantsHasNoValue() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setEnforcementOrder(EnforcementOrder.builder()
            .rawWarrantDetails(RawWarrantDetails.builder()
                                   .selectedDefendants(DynamicMultiSelectStringList.builder()
                                                           .listItems(List.of())
                                                           .build())
                                   .build())
            .build());

        // When
        List<SelectedDefendantEntity> entities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        // Then
        assertThat(entities).isEmpty();
    }
}
