package uk.gov.hmcts.reform.pcs.functional.tests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("Functional")
class PipelineFailureProbeTest {
    @Test
    void failsToExercisePipelineFailureHandling() {
        fail("Temporary probe: remove after checking the pipeline");
    }
}