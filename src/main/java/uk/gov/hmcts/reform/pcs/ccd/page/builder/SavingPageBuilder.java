package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.Optional;

public class SavingPageBuilder extends PageBuilder {

    private final DraftCaseDataService draftCaseDataService;
    private final EventId eventId;

    public SavingPageBuilder(DraftCaseDataService draftCaseDataService,
                             EventBuilder<PCSCase, UserRole, State> eventBuilder, EventId eventId) {

        super(eventBuilder);
        this.draftCaseDataService = draftCaseDataService;
        this.eventId = eventId;
    }

    @Override
    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(String id) {
        SavingMidEventDecorator savingMidEventDecorator = new SavingMidEventDecorator(eventId);
        return super.page(id, savingMidEventDecorator::handleMidEvent);
    }

    @Override
    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(
        String id, MidEvent<PCSCase, State> midEventCallback) {

        SavingMidEventDecorator savingMidEventDecorator = new SavingMidEventDecorator(midEventCallback, eventId);
        return super.page(id, savingMidEventDecorator::handleMidEvent);
    }


    private class SavingMidEventDecorator {

        private final MidEvent<PCSCase, State> wrappedMidEvent;
        private final EventId caseEventId;

        public SavingMidEventDecorator(EventId eventId) {
            this.wrappedMidEvent = null;
            this.caseEventId = eventId;
        }

        public SavingMidEventDecorator(MidEvent<PCSCase, State> midEvent, EventId eventId) {
            this.wrappedMidEvent = midEvent;
            this.caseEventId = eventId;
        }

        public AboutToStartOrSubmitResponse<PCSCase, State> handleMidEvent(CaseDetails<PCSCase, State> details,
                                                                           CaseDetails<PCSCase, State> detailsBefore) {

            AboutToStartOrSubmitResponse<PCSCase, State> wrappedMidEventResponse = null;
            if (wrappedMidEvent != null) {
                wrappedMidEventResponse = wrappedMidEvent.handle(details, detailsBefore);
            }

            patchUnsubmittedData(details);

            removeTransientFieldsFromResponse(details, wrappedMidEventResponse);

            return Optional.ofNullable(wrappedMidEventResponse)
                .orElseGet(() -> AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .data(details.getData())
                    .build());
        }

        private void patchUnsubmittedData(CaseDetails<PCSCase, State> details) {
            long caseReference = details.getId();
            PCSCase caseData = details.getData();

            draftCaseDataService.patchUnsubmittedEventData(caseReference, caseData, caseEventId);
        }

        /**
         * Nulls clearFields on the in-memory PCSCase before the response is serialised back to CCD.
         *
         * <p>DraftClearFieldsProcessor already strips clearFields from the persisted draft JSON,
         * but the in-memory Java object still holds the value. {@code @CCD(ignore = true)} only
         * excludes the field from CCD's schema definition — Jackson still serialises it in the
         * callback response. CCD rejects unknown fields not in its schema, so we must null it here.
         */
        private void removeTransientFieldsFromResponse(CaseDetails<PCSCase, State> details,
                                                       AboutToStartOrSubmitResponse<PCSCase, State> response) {
            PCSCase detailsData = details.getData();
            PCSCase responseData = response != null ? response.getData() : null;

            if (detailsData != null) {
                detailsData.setClearFields(null);
            }
            if (responseData != null) {
                responseData.setClearFields(null);
            }
        }

    }

}
