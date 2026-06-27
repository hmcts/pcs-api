package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TEMPORARY diagnostic - logs the bulk-print flag on a timer so it can be flipped in LaunchDarkly
 * and verified live. Remove before merging the bulk-print consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureFlagLogger {

    private static final long LOG_INTERVAL_MS = 15_000;

    private final FeatureToggleService featureToggle;

    @Scheduled(fixedRate = LOG_INTERVAL_MS)
    public void logBulkPrintFlag() {
        log.info("[LD-TEST] bulk-print-enabled = {}", featureToggle.isEnabled(FeatureFlag.BULK_PRINT));
    }
}
