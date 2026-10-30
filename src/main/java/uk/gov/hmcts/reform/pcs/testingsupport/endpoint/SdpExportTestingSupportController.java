package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.sdp.ExportResult;
import uk.gov.hmcts.reform.pcs.sdp.ExportService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Map;


@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class SdpExportTestingSupportController {

    private final ExportService exportService;
    private final FeatureToggleService featureToggleService;

    public SdpExportTestingSupportController(ExportService exportService,
                                             FeatureToggleService featureToggleService) {
        this.exportService = exportService;
        this.featureToggleService = featureToggleService;
    }

    @PostMapping("/sdp/export-claim-snapshots")
    public ResponseEntity<Map<String, Object>> exportClaimSnapshots() {
        if (!featureToggleService.isEnabled(FeatureFlag.SDP_EXPORT)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        ExportResult result = exportService.exportClaims();
        return ResponseEntity.ok(Map.of(
            "filename", result.filename(),
            "recordCount", result.recordCount()
        ));
    }
}
