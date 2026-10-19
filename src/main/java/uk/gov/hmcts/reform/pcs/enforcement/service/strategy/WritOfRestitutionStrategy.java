package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WritOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WritOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatementOfTruthService;

@Component
@AllArgsConstructor
public class WritOfRestitutionStrategy implements EnforcementTypeStrategy {

    private final WritOfRestitutionRepository writOfRestitutionRepository;
    private final EnforcementStatementOfTruthService enforcementStatementOfTruthService;

    /**
     * The journey asks which language the claimant used and takes the statement of truth on the writ journey's
     * pages, so both answers are on {@link WritDetails}.
     */
    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        WritDetails writDetails = enforcementOrder.getWritDetails();
        WritOfRestitutionEntity writOfRestitutionEntity = new WritOfRestitutionEntity();
        writOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        if (writDetails != null) {
            writOfRestitutionEntity.setLanguageUsed(writDetails.getLanguageUsed());
        }
        writOfRestitutionRepository.save(writOfRestitutionEntity);
        enforcementStatementOfTruthService.processStatementOfTruth(
                writDetails == null ? null : writDetails.getStatementOfTruth(), enforcementOrderEntity);
    }

}
