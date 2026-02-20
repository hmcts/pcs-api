package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.SelectedDefendantsMapper;
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
        DynamicStringListElement def1 = new DynamicStringListElement(
            jessMayID.toString(), "Jess May");
        DynamicStringListElement def2 = new DynamicStringListElement(
            jamesMayID.toString(), "James May");

        DynamicMultiSelectStringList selectedDefendants = new DynamicMultiSelectStringList(
            List.of(def1, def2), List.of(def1, def2)
        );

        RawWarrantDetails rawWarrantDetails = RawWarrantDetails.builder()
            .selectedDefendants(selectedDefendants)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .rawWarrantDetails(rawWarrantDetails)
            .build();

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
            .allSatisfy(e -> assertThat(e.getEnforcementCase())
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
}
