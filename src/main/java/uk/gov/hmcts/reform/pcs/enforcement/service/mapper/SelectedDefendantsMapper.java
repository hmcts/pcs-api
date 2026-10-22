package uk.gov.hmcts.reform.pcs.enforcement.service.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Component
public class SelectedDefendantsMapper {

    private final PartyRepository partyRepository;

    public List<SelectedDefendantEntity> mapToEntities(
        EnforcementOrderEntity enforcementOrderEntity) {

        EnforcementOrder enforcementOrder = enforcementOrderEntity.getEnforcementOrder();
        RawWarrantDetails rawWarrantDetails =
            enforcementOrder == null ? null : enforcementOrder.getRawWarrantDetails();
        DynamicMultiSelectStringList selectedDefendants =
            rawWarrantDetails == null ? null : rawWarrantDetails.getSelectedDefendants();

        if (selectedDefendants == null || selectedDefendants.getValue() == null) {
            return Collections.emptyList();
        }

        List<UUID> selectedIds = selectedDefendants.getValue()
            .stream()
            .map(DynamicStringListElement::getCode)
            .map(UUID::fromString)
            .toList();

        List<PartyEntity> parties = partyRepository.findAllById(selectedIds);

        return parties.stream()
            .map(party -> SelectedDefendantEntity.builder()
                .enfCase(enforcementOrderEntity)
                .party(party)
                .build()
            )
            .toList();
    }
}

