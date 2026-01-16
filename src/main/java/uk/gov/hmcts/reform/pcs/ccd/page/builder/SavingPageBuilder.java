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
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;

public class SavingPageBuilder extends PageBuilder {

    private final DraftCaseDataService draftCaseDataService;
    private final SecurityContextService securityContextService;
    private final EventId eventId;

    public SavingPageBuilder(DraftCaseDataService draftCaseDataService,
                             SecurityContextService securityContextService,
                             EventBuilder<PCSCase, UserRole, State> eventBuilder, EventId eventId) {

        super(eventBuilder);
        this.draftCaseDataService = draftCaseDataService;
        this.securityContextService = securityContextService;
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

    }

}
