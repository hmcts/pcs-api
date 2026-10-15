package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.sdp.ExportResult;
import uk.gov.hmcts.reform.pcs.sdp.ExportService;

import java.util.Map;


@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class SdpExportTestingSupportController {

    private final ExportService exportService;

    public SdpExportTestingSupportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/sdp/export-claim-snapshots")
    public ResponseEntity<Map<String, Object>> exportClaimSnapshots() {
        ExportResult result = exportService.exportAllClaims();
        return ResponseEntity.ok(Map.of(
            "outputPath", result.outputPath().toAbsolutePath().toString(),
            "recordCount", result.recordCount()
        ));
    }
}
