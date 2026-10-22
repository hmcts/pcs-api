package uk.gov.hmcts.reform.pcs.camunda;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Identifies the one work allocation request a change makes, so the request can be scheduled once however
 * many times the change is retried. {@link CamundaService} uses it as the db-scheduler instance id for the
 * request and as the seed for the request's idempotency key, which is what the task-initiation BPMN's
 * idempotency gateway checks.
 *
 * <p>Derive it from the row the change's own transaction wrote, not from the thing the task is about: a
 * reopened warrant has to be able to get a second "Allocate case to bailiff", so that reference is the
 * warrant return that reopened it rather than the warrant.</p>
 */
public record TaskReference(String value) {

    private static final String PART_SEPARATOR = "-";

    public TaskReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A task reference needs a value");
        }
    }

    /**
     * A reference made of the parts that make the change unique, for example the id of the row it wrote.
     */
    public static TaskReference of(Object... parts) {
        return new TaskReference(Arrays.stream(parts)
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(PART_SEPARATOR)));
    }

    @Override
    public String toString() {
        return value;
    }
}
