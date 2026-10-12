package uk.gov.hmcts.reform.pcs.sdp;

import java.nio.file.Path;

/**
 * Outcome of a local SDP claim snapshot export run.
 */
public record ClaimSnapshotExportResult(Path outputPath, int recordCount) {
}
