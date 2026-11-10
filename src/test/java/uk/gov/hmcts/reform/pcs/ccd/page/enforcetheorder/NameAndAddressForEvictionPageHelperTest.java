package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.NameAndAddressForEvictionPageHelper.MAX_UNKNOWN_DEFENDANTS;

@ExtendWith(MockitoExtension.class)
public class NameAndAddressForEvictionPageHelperTest {

    private NameAndAddressForEvictionPageHelper underTest;

    @BeforeEach
    void setUp() {
        underTest = new NameAndAddressForEvictionPageHelper();
    }

    @Test
    void shouldShowFirstUnknownDefendantForAnyCount() {
        assertThat(underTest.unknownDefendantShowCondition(0))
            .startsWith("unknownDefendantCount=\"1\" OR unknownDefendantCount=\"2\" OR ")
            .endsWith(" OR unknownDefendantCount=\"" + MAX_UNKNOWN_DEFENDANTS + "\"");
    }

    @Test
    void shouldShowThirdUnknownDefendantOnlyFromThreeUnknownDefendants() {
        assertThat(underTest.unknownDefendantShowCondition(2))
            .startsWith("unknownDefendantCount=\"3\" OR unknownDefendantCount=\"4\"")
            .doesNotContain("unknownDefendantCount=\"2\"");
    }

    @Test
    void shouldShowLastUnknownDefendantOnlyAtMaximumCount() {
        assertThat(underTest.unknownDefendantShowCondition(MAX_UNKNOWN_DEFENDANTS - 1))
            .isEqualTo("unknownDefendantCount=\"" + MAX_UNKNOWN_DEFENDANTS + "\"");
    }
}
