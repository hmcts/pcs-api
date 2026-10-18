package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import java.nio.file.Path;

/**
 * A finished batch print.
 *
 * @param pdf the temp file holding every printed case's documents in one PDF, or null when no case could be
 *            printed. Whoever asked for the print owns the file and has to delete it:
 *            {@link BatchPrintFileCleanupFilter} does that for a request.
 */
public record BatchPrint(Path pdf, BatchPrintPlan plan) {

    public boolean isEmpty() {
        return pdf == null;
    }
}
