package uk.gov.hmcts.reform.pcs.camunda;

import java.util.UUID;

/**
 * What a task is about, for a task there can be one of per order or per general application on a case:
 * the task variable that names it, which the configuration DMN makes an additional property of the
 * task, and its id.
 */
public record TaskSubject(String property, UUID id) {

    public static TaskSubject order(UUID orderId) {
        return new TaskSubject("orderId", orderId);
    }

    public static TaskSubject genApp(UUID genAppId) {
        return new TaskSubject("genAppId", genAppId);
    }
}
