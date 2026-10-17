package uk.gov.hmcts.reform.pcs.enforcement.service;

import java.util.List;

/**
 * What an enforcement service did when an event's submit handler asked it to change the claim's enforcement:
 * the change it made, or the errors to show the user instead.
 *
 * <p>A decentralised event's submit handler runs inside the transaction the SDK's case event coordinator
 * opens, and an exception out of a {@code @Transactional} service method marks that transaction
 * rollback-only. The SDK's {@code DecentralisedSubmissionHandler} rolls the event back itself whenever the
 * handler answers with errors, so a handler that caught the exception and returned those errors got away with
 * it; a handler that swallowed the refusal and went on to answer successfully would instead fail the
 * coordinator's commit with {@code UnexpectedRollbackException}, giving the caseworker a 500 in place of
 * anything the handler said. Keeping the refusal in the return type keeps it out of that reach.</p>
 *
 * <p>Start callbacks and mid-event pages run through {@code CallbackController}, which
 * {@code EnforcementCallbackExceptionHandler} is scoped to, so those callers still refuse by throwing.</p>
 *
 * @param change what the service changed, for the confirmation screen. Null when the change was refused, and
 *               when there is nothing to report about a change that was made.
 * @param errors why the change was refused, empty when it was made
 */
public record EnforcementChange<T>(T change, List<String> errors) {

    public EnforcementChange {
        errors = List.copyOf(errors);
    }

    /**
     * A change that was made and has nothing to report.
     */
    public static EnforcementChange<Void> made() {
        return new EnforcementChange<>(null, List.of());
    }

    public static <T> EnforcementChange<T> made(T change) {
        return new EnforcementChange<>(change, List.of());
    }

    public static <T> EnforcementChange<T> refused(String error) {
        return refused(List.of(error));
    }

    public static <T> EnforcementChange<T> refused(List<String> errors) {
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("A refused change needs at least one error");
        }
        return new EnforcementChange<>(null, errors);
    }

    public boolean refused() {
        return !errors.isEmpty();
    }
}
