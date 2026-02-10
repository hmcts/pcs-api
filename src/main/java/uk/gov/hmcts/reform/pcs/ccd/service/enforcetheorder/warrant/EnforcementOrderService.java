package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementSelectedDefendantRepository enforcementSelectedDefendantRepository;
    private final PartyRepository partyRepository;

    public EnforcementOrderEntity loadEnforcementOrder(UUID id) {
        return enforcementOrderRepository.findById(id)
                .orElseThrow(() -> new EnforcementOrderNotFoundException(id));
    }

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow(() -> new CaseNotFoundException(caseReference));

        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();
        // This should never happen
        if (CollectionUtils.isEmpty(claimEntities)) {
            log.error("No claim found for case reference {}", caseReference);
            throw new ClaimNotFoundException(pcsCaseEntity.getCaseReference());
        }

        // Assuming 1 claim per PcsCase
        ClaimEntity claimEntity = claimEntities.getFirst();

        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);


        enforcementOrderRepository.save(enforcementOrderEntity);

        if(enforcementOrderEntity.getEnforcementOrder().getRawWarrantDetails().getSelectedDefendants() != null) {
            DynamicMultiSelectStringList selectedDefendants = getSelectedDefendants(enforcementOrderEntity);

            List<PartyEntity> parties = getPartyEntities(selectedDefendants);

            linkSelectedDefendantsToEnforcementOrder(parties, enforcementOrderEntity);
        }
    }

    private void linkSelectedDefendantsToEnforcementOrder(List<PartyEntity> parties, EnforcementOrderEntity enforcementOrderEntity) {
        for (PartyEntity party : parties) {
            EnforcementSelectedDefendantEntity entity = new EnforcementSelectedDefendantEntity();
            entity.setEnforcementCase(enforcementOrderEntity);
            entity.setParty(party);
            enforcementSelectedDefendantRepository.save(entity);
        }
    }

    private List<PartyEntity> getPartyEntities(DynamicMultiSelectStringList selectedDefendants) {
        List<UUID> selectedDefIds = selectedDefendants.getValue()
            .stream()
            .map(DynamicStringListElement::getCode)
            .map(UUID::fromString)
            .toList();

        return partyRepository.findAllById(selectedDefIds);
    }

    private DynamicMultiSelectStringList getSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        return enforcementOrderEntity
            .getEnforcementOrder()
            .getRawWarrantDetails()
            .getSelectedDefendants();
    }
}
