package uk.gov.hmcts.reform.pcs.ccd.event.breathingspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@ExtendWith(MockitoExtension.class)
class LiftBreathingSpaceTest extends BaseEventTest {

    @InjectMocks
    private LiftBreathingSpace liftBreathingSpace;

    @BeforeEach
    void setUP() {
        setEventUnderTest(liftBreathingSpace);
    }

    @Test
    void shouldOnlyShowWhenFeatureFlagEnabledAndBreathingSpaceActive() {
        assertConfiguredShowConditions(ShowConditions.and(
            ShowConditions.featureFlagsEnabled(ENFORCEMENT),
            "hasActiveBreathingSpace=\"Yes\""
        ));
    }

}
