package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

final class SummaryFields {

    private SummaryFields() {
    }

    /**
     * Drops the event's {@code NEVER_SHOW} fields from check your answers, which the SDK overloads taking a
     * {@code retainHiddenValue} flag otherwise put there. Field placements belong to one event, so every
     * event that reuses a page configurer and calls {@code showSummary()} has to call this for itself.
     */
    static void excludeNeverShownFields(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        excludeNeverShownFields(eventBuilder.fields().build());
    }

    private static void excludeNeverShownFields(FieldCollection fieldCollection) {
        fieldCollection.getFields().stream()
            .filter(field -> NEVER_SHOW.equals(field.build().getShowCondition()))
            .forEach(field -> field.showSummary(false));
        fieldCollection.getComplexFields()
            .forEach(complexField -> excludeNeverShownFields(complexField.build()));
    }
}
