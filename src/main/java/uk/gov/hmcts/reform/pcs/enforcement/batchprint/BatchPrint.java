package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import java.nio.file.Path;

public record BatchPrint(Path pdf, BatchPrintPlan plan, Path directory) {

    public boolean isEmpty() {
        return pdf == null;
    }
}
