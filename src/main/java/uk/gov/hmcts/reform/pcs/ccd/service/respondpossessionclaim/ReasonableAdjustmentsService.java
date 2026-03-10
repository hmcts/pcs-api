package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ReasonableAdjustments;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.ReasonableAdjustmentEntity;

@Service
public class ReasonableAdjustmentsService {

    public ReasonableAdjustmentEntity createReasonableAdjustmentEntity(ReasonableAdjustments reasonableAdjustments) {

        if (reasonableAdjustments == null) {
            return null;
        }

        ReasonableAdjustmentEntity reasonableAdjustmentEntity = ReasonableAdjustmentEntity.builder()
            .reasonableAdjustmentsRequired(reasonableAdjustments.getReasonableAdjustmentRequired())
            .build();

        return reasonableAdjustmentEntity;
    }

}
