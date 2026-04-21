package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClearFields {

    private final PCSCase caseData;
    private final List<String> fields = new ArrayList<>();

    private ClearFields(PCSCase caseData) {
        this.caseData = caseData;
    }

    public static ClearFields on(PCSCase caseData) {
        return new ClearFields(caseData);
    }

    public ClearFields clearWhen(boolean condition, String... fieldPaths) {
        if (condition) {
            Collections.addAll(fields, fieldPaths);
        }
        return this;
    }

    public void apply() {
        if (!fields.isEmpty()) {
            caseData.setClearFields(fields);
        }
    }
}
