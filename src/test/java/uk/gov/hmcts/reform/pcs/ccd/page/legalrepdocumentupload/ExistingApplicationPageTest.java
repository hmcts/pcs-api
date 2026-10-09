package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

class ExistingApplicationPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ExistingApplicationPage());
    }

    @Test
    void shouldShowApplicationDocumentLinksOnlyWhenRelease1Dot4IsEnabled() {
        List<Field> fields = event.getFields().getFields().stream()
            .map(Field.FieldBuilder::build)
            .toList();

        assertThat(fields)
            .filteredOn(field -> "existingApplication-application-links".equals(field.getId()))
            .singleElement()
            .satisfies(field -> assertThat(field.getShowCondition())
                .isEqualTo("featureFlags.release1dot4Enabled=\"YES\""));
    }

    @Test
    void shouldRetainFeatureFlagsAsHiddenInternalContext() {
        List<Field> fields = event.getFields().getFields().stream()
            .map(Field.FieldBuilder::build)
            .toList();

        assertThat(fields)
            .filteredOn(field -> "featureFlags".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getShowCondition()).isEqualTo(NEVER_SHOW);
                assertThat(field.getContext()).isEqualTo(DisplayContext.ReadOnly);
                assertThat(field.isRetainHiddenValue()).isTrue();
            });
    }
}
