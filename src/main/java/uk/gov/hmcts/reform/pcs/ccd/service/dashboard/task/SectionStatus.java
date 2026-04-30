package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.Arrays;
import java.util.Optional;

public enum SectionStatus {
    IN_PROGRESS,
    COMPLETED;

    public static Optional<SectionStatus> from(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(v -> v.name().equals(rawValue))
            .findFirst();
    }
}
