package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;

import java.util.Arrays;
import java.util.Optional;

final class BulkFailReasonCodes {

    private BulkFailReasonCodes() {
    }

    static Optional<BulkFailReason> forFailReasonCode(Integer failReasonCode) {
        if (failReasonCode == null) {
            return Optional.empty();
        }
        return Arrays.stream(BulkFailReason.values())
            .filter(reason -> failReasonCode.equals(reason.getFailReasonCode()))
            .findFirst();
    }
}
