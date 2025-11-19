package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.warrant.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcement.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;

import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseRepository pcsCaseRepository;

    public EnforcementOrderService(EnforcementOrderRepository enforcementOrderRepository,
                                   PcsCaseRepository pcsCaseRepository) {
        this.enforcementOrderRepository = enforcementOrderRepository;
        this.pcsCaseRepository = pcsCaseRepository;
    }

    public EnforcementOrderEntity loadEnforcementOrder(UUID id) {
        return enforcementOrderRepository.findById(id)
                .orElseThrow(() -> new EnforcementOrderNotFoundException(id));
    }

    public void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow(() -> new CaseNotFoundException(caseReference));

        Set<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();
        // This should never happen
        if (claimEntities.isEmpty()) {
            log.error("No claim found for case reference {}", caseReference);
            throw new ClaimNotFoundException(pcsCaseEntity.getCaseReference());
        }

        // Assuming 1 claim per PcsCase
        ClaimEntity claimEntity = claimEntities.iterator().next();

        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);

        enforcementOrderRepository.save(enforcementOrderEntity);
        log.debug("Created Enforcement Order for case reference {}", caseReference);
    }
}
