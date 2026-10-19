package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import java.nio.file.Path;

/**
 * A finished batch print.
 *
 * @param pdf the temp file holding every printed case's documents in one PDF, or null when no case could be
 *            printed
 * @param plan the cases printed and the cases left out, with the reason for each
 * @param directory the directory the PDF sits in, or null when there is no PDF. Whoever asked for the print
 *                  owns the directory and has to delete it: {@link BatchPrintFileCleanupFilter} does that for
 *                  a request.
 */
public record BatchPrint(Path pdf, BatchPrintPlan plan, Path directory) {

    public boolean isEmpty() {
        return pdf == null;
    }
}
