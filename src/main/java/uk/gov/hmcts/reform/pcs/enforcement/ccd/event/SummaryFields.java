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
     * Drops every {@code NEVER_SHOW} field of the event from its check-your-answers summary.
     *
     * <p>A field placed to carry a value or to feed a {@code ${...}} label is hidden with
     * {@code NEVER_SHOW}, but the SDK overloads that take a {@code retainHiddenValue} flag also set
     * {@code ShowSummary=Y}, which puts the raw value on check your answers. Field placements belong to one
     * event, so every event that reuses a page configurer and calls {@code showSummary()} has to call this
     * for itself.</p>
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
