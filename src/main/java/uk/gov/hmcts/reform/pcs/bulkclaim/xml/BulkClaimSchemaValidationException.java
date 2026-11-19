package uk.gov.hmcts.reform.pcs.bulkclaim.xml;

import java.util.List;

public class BulkClaimSchemaValidationException extends RuntimeException {

    private final transient List<String> errors;

    public BulkClaimSchemaValidationException(List<String> errors) {
        super("Bulk claim XML failed schema validation: " + errors.size() + " error(s)");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
