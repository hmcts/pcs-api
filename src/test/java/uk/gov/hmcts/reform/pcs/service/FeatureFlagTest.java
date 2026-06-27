package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagTest {

    @Test
    void flagKeysAreUniqueNonBlankAndKebabCase() {
        List<String> keys = Arrays.stream(FeatureFlag.values()).map(FeatureFlag::key).toList();

        assertThat(keys).doesNotHaveDuplicates();
        assertThat(keys).allSatisfy(key ->
            assertThat(key).isNotBlank().matches("[a-z0-9]+(-[a-z0-9]+)*"));
    }
}
