package uk.gov.hmcts.reform.pcs.ccd.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ClaimPackSender;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.DefencePackSender;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class BulkPrintGenerationComponentTest {

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;
    @Mock
    private ClaimPackSender claimPackSender;
    @Mock
    private DefencePackSender defencePackSender;

    @Test
    void buildsTaskWithNightlyDefaultSchedule() {
        assertThat(componentWithSchedule("DAILY|02:00").bulkPrintTask()).isNotNull();
    }

    @Test
    void parsesFixedDelayEnvironmentSchedules() {
        assertThatCode(() -> componentWithSchedule("FIXED_DELAY|60s").bulkPrintTask()).doesNotThrowAnyException();
        assertThatCode(() -> componentWithSchedule("FIXED_DELAY|600s").bulkPrintTask()).doesNotThrowAnyException();
    }

    private BulkPrintGenerationComponent componentWithSchedule(String schedule) {
        return new BulkPrintGenerationComponent(
            featureToggleService, claimActivityLogRepository, claimPackSender, defencePackSender, schedule);
    }
}
