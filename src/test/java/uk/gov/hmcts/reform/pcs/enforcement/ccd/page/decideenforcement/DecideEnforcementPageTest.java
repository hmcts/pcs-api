package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.decideenforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.Field.FieldBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecideEnforcementPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new DecideEnforcementPage());
    }

    @Test
    void shouldPlaceThePropertyAddressAndPartiesBeforeTheCaseDetailsThatShowThem() {
        List<String> fieldOrder = pageFields().stream().map(Field::getId).toList();

        assertThat(fieldOrder).containsExactly(
            "formattedPropertyAddress",
            "formattedCaseName",
            "decideEnforcement-caseDetails",
            "enforcementDecision",
            "enforcementRejectionReason"
        );
        assertFieldsArePlacedBeforeTheLabelsThatShowThem();
    }

    @Test
    void shouldCarryTheCaseDetailsHiddenAndShowThemThroughTheLabel() {
        Field<?, ?, ?, ?> label = field("decideEnforcement-caseDetails");

        assertThat(label.getLabel()).contains("${formattedPropertyAddress}", "${formattedCaseName}");
        assertThat(label.isShowSummary()).isTrue();
        assertThat(List.of(field("formattedPropertyAddress"), field("formattedCaseName")))
            .allSatisfy(carrier -> {
                assertThat(carrier.getContext()).isEqualTo(DisplayContext.ReadOnly);
                assertThat(carrier.getShowCondition()).isEqualTo("[STATE]=\"NEVER_SHOW\"");
                assertThat(carrier.isRetainHiddenValue()).isTrue();
            });
    }

    private Field<?, ?, ?, ?> field(String id) {
        return pageFields().stream().filter(field -> id.equals(field.getId())).findFirst().orElseThrow();
    }

    private List<Field<?, ?, ?, ?>> pageFields() {
        return event.getFields().getFields().stream()
            .map(FieldBuilder::build)
            .<Field<?, ?, ?, ?>>map(field -> field)
            .sorted(Comparator.comparingInt(Field::getPageFieldDisplayOrder))
            .toList();
    }
}
