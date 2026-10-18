package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskAssessmentUpdateEntity;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentUpdateRepository extends JpaRepository<RiskAssessmentUpdateEntity, UUID> {

    /**
     * The enforcement's most recent risk assessment update, which holds its current answers.
     */
    Optional<RiskAssessmentUpdateEntity> findFirstByEnforcementOrderIdOrderByUpdatedAtDesc(UUID enforcementOrderId);
}
