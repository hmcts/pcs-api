package uk.gov.hmcts.reform.pcs.enforcement.exception;

import lombok.Getter;

import java.util.List;

/**
 * Thrown on submit of "Record warrant return" when the answers are not valid for the warrant as it is now,
 * for example because another bailiff closed it while the event was open. The errors are shown to the user.
 */
@Getter
public class WarrantReturnValidationException extends RuntimeException {

    private final List<String> errors;

    public WarrantReturnValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }
}
