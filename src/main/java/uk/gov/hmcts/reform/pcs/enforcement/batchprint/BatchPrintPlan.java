package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import java.util.ArrayList;
import java.util.List;

/**
 * What a batch print contains: the printed cases in the order the bailiff chose them, and the cases left out.
 */
public record BatchPrintPlan(List<PrintableCase> included, List<ExcludedCase> excluded) {

    /**
     * This plan with {@code dropped} moved from the printed cases to the excluded ones.
     */
    public BatchPrintPlan exclude(List<ExcludedCase> dropped) {
        if (dropped.isEmpty()) {
            return this;
        }
        List<String> droppedReferences = dropped.stream().map(ExcludedCase::caseReference).toList();
        List<ExcludedCase> allExcluded = new ArrayList<>(excluded);
        allExcluded.addAll(dropped);
        return new BatchPrintPlan(
            included.stream().filter(printable -> !droppedReferences.contains(printable.caseReference())).toList(),
            List.copyOf(allExcluded));
    }
}
