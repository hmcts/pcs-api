package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.Field.FieldBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

class EvictionDatePageTest extends BasePageTest {

    private final EvictionDatePage pageUnderTest = new EvictionDatePage();

    @BeforeEach
    void setUp() {
        setPageUnderTest(pageUnderTest);
    }

    @Test
    void shouldShowTheClaimantTheDateTheBailiffGaveThem() {
        List<String> evictionDateLabels = pageFields()
            .filter(field -> (pageUnderTest.getPageKey() + "-eviction-date").equals(field.getId()))
            .map(Field::getLabel)
            .toList();

        assertThat(evictionDateLabels).singleElement(STRING).contains("${formattedEvictionDate}");
    }

    @Test
    void shouldCarryTheEvictionDateIntoTheEventAlongsideTheQuestion() {
        assertThat(pageFields().map(Field::getId).toList())
            .contains("formattedEvictionDate", "claimantCanAttendEviction");
    }

    private Stream<Field<?, ?, ?, ?>> pageFields() {
        return event.getFields().getFields().stream()
            .map(FieldBuilder::build)
            .map(field -> (Field<?, ?, ?, ?>) field);
    }
}
