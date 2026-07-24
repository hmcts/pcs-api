package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

class ManageHearingPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ManageHearingPage());
    }

    @Test
    void shouldRetainSelectedHearingIdAsHiddenInternalContext() {
        List<Field> fields = event.getFields().getFields().stream()
            .map(Field.FieldBuilder::build)
            .toList();

        assertThat(fields)
            .filteredOn(field -> "selectedHearingId".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getShowCondition()).isEqualTo(NEVER_SHOW);
                assertThat(field.getContext()).isEqualTo(DisplayContext.ReadOnly);
                assertThat(field.isRetainHiddenValue()).isTrue();
            });
    }
}
