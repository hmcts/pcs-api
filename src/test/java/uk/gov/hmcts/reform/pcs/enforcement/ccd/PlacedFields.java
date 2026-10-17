package uk.gov.hmcts.reform.pcs.enforcement.ccd;

import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;

import java.util.Optional;

/**
 * Finds a field an event or page has placed, wherever it sits in the complex-type tree.
 */
public final class PlacedFields {

    private PlacedFields() {
    }

    public static Field<?, ?, ?, ?> find(FieldCollection fieldCollection, String fieldId) {
        return findIn(fieldCollection, fieldId)
            .orElseThrow(() -> new AssertionError("No field placed with ID " + fieldId));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<Field<?, ?, ?, ?>> findIn(FieldCollection fieldCollection, String fieldId) {
        Optional<Field<?, ?, ?, ?>> match = fieldCollection.getFields().stream()
            .<Field<?, ?, ?, ?>>map(fieldBuilder -> (Field<?, ?, ?, ?>) fieldBuilder.build())
            .filter(field -> fieldId.equals(field.getId()))
            .findFirst();

        if (match.isPresent()) {
            return match;
        }

        return fieldCollection.getComplexFields().stream()
            .map(nested -> findIn(((FieldCollectionBuilder) nested).build(), fieldId))
            .flatMap(Optional::stream)
            .findFirst();
    }
}
