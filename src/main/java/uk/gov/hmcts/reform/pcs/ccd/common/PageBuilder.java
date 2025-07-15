package uk.gov.hmcts.reform.pcs.ccd.common;

import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class PageBuilder {

    private final EventBuilder<PCSCase, UserRole, State> eventBuilder;

    public PageBuilder(EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        this.eventBuilder = eventBuilder;
    }

    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(final String id) {
        return eventBuilder.fields().page(id);
    }

    public PageBuilder add(CcdPageConfiguration pageConfiguration) {
        pageConfiguration.addTo(this);
        return this;
    }
}
