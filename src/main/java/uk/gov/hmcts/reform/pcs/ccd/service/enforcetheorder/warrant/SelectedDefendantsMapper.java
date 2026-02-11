package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementSelectedDefendantEntity;
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

    public List<EnforcementSelectedDefendantEntity> mapToEntities(
        EnforcementOrderEntity enforcementOrderEntity) {

        DynamicMultiSelectStringList selectedDefendants =
            enforcementOrderEntity.getEnforcementOrder().getRawWarrantDetails().getSelectedDefendants();

        if (selectedDefendants == null) {
            return Collections.emptyList();
        }

        List<UUID> selectedIds = selectedDefendants.getValue()
            .stream()
            .map(DynamicStringListElement::getCode)
            .map(UUID::fromString)
            .toList();

        List<PartyEntity> parties = partyRepository.findAllById(selectedIds);

        return parties.stream()
            .map(party -> {
                EnforcementSelectedDefendantEntity entity = new EnforcementSelectedDefendantEntity();
                entity.setEnforcementCase(enforcementOrderEntity);
                entity.setParty(party);
                return entity;
            })
            .toList();
    }
}

