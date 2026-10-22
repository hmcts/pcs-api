package uk.gov.hmcts.reform.pcs.enforcement.service;

import java.util.List;

/**
 * The change an enforcement service made for a submit handler, or the errors to show the user instead. The
 * refusal is in the return type because an exception would mark the SDK's event transaction rollback-only.
 */
public record EnforcementChange<T>(T change, List<String> errors) {

    public EnforcementChange {
        errors = List.copyOf(errors);
    }

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
