package uk.gov.hmcts.reform.pcs.sdp;

import java.nio.file.Path;

public record ExportResult(Path outputPath, int recordCount) {
}
