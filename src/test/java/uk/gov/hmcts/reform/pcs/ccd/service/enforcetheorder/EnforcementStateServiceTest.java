package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementStateEntity;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementStateServiceTest {

    private final EnforcementStateService underTest = new EnforcementStateService();

    @Test
    void shouldCreateAndAssociateEnforcementStateEntity() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        underTest.mapToEntity(pcsCaseEntity, EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);

        EnforcementStateEntity enforcementState = pcsCaseEntity.getEnforcementState();
        assertThat(enforcementState).isNotNull();
        assertThat(enforcementState.getState()).isEqualTo(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);
        assertThat(enforcementState.getPcsCase()).isSameAs(pcsCaseEntity);
    }

    @Test
    void shouldUpdateExistingEnforcementStateEntity() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        EnforcementStateEntity existingState = EnforcementStateEntity.builder()
            .pcsCase(pcsCaseEntity)
            .state(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS)
            .build();
        pcsCaseEntity.setEnforcementState(existingState);

        underTest.mapToEntity(pcsCaseEntity, EnforcementState.PENDING_CASE_ISSUED);

        assertThat(pcsCaseEntity.getEnforcementState()).isSameAs(existingState);
        assertThat(existingState.getState()).isEqualTo(EnforcementState.PENDING_CASE_ISSUED);
        assertThat(existingState.getPcsCase()).isSameAs(pcsCaseEntity);
    }
}
