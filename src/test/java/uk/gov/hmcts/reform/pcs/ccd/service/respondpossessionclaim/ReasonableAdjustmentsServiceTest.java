package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ReasonableAdjustments;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.ReasonableAdjustmentEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ReasonableAdjustmentsServiceTest {

    private final ReasonableAdjustmentsService service = new ReasonableAdjustmentsService();

    @Test
    void shouldMapReasonableAdjustmentsRequiredField() {
        ReasonableAdjustments model = ReasonableAdjustments.builder()
            .reasonableAdjustmentRequired("Wheelchair access")
            .build();

        ReasonableAdjustmentEntity entity = service.createReasonableAdjustmentEntity(model);

        assertThat(entity.getReasonableAdjustmentsRequired()).isEqualTo("Wheelchair access");
    }

}

