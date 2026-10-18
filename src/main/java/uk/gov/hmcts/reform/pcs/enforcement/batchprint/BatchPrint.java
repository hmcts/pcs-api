package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.springframework.core.io.Resource;

/**
 * A finished batch print.
 *
 * @param pdf every printed case's documents in one PDF, readable once, or null when no case could be printed
 */
public record BatchPrint(Resource pdf, BatchPrintPlan plan) {

    public boolean isEmpty() {
        return pdf == null;
    }
}
