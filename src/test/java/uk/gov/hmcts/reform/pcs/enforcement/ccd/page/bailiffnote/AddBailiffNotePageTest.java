package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AddBailiffNotePageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new AddBailiffNotePage());
    }

    @Test
    void shouldAskForAMandatoryNoteOnTheAddBailiffNotePage() {
        Field<?, ?, ?, ?> noteField = allFields()
            .filter(field -> "bailiffNoteText".equals(field.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(noteField.getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(noteField.getPage()).isEqualTo("addBailiffNote");
        assertThat(event.getFields().getPageLabels()).containsEntry("addBailiffNote", "Add bailiff note");
    }

    @Test
    void shouldHaveNoMidEvent() {
        assertThat(event.getFields().getPagesToMidEvent()).isEmpty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Stream<Field<?, ?, ?, ?>> allFields() {
        Stream<FieldCollection> complexFields = event.getFields().getComplexFields().stream()
            .map(builder -> ((FieldCollectionBuilder) builder).build());
        return Stream.concat(Stream.of(event.getFields()), complexFields)
            .flatMap(collection -> ((FieldCollection) collection).getFields().stream())
            .map(builder -> (Field<?, ?, ?, ?>) ((Field.FieldBuilder) builder).build());
    }
}
