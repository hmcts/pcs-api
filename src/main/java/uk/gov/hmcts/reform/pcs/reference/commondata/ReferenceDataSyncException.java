package uk.gov.hmcts.reform.pcs.reference.commondata;

import java.util.List;

public class ReferenceDataSyncException extends RuntimeException {

    public ReferenceDataSyncException(List<String> failedCategories) {
        super("Reference data synchronisation failed for categories: " + String.join(", ", failedCategories));
    }
}
