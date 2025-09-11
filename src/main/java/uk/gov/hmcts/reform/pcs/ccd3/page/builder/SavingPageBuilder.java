package uk.gov.hmcts.reform.pcs.ccd3.page.builder;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.service.UnsubmittedCaseDataService;

import java.util.Optional;

public class SavingPageBuilder extends PageBuilder {

    private final UnsubmittedCaseDataService unsubmittedCaseDataService;

    public SavingPageBuilder(UnsubmittedCaseDataService unsubmittedCaseDataService,
                             EventBuilder<PCSCase, UserRole, State> eventBuilder) {

        super(eventBuilder);
        this.unsubmittedCaseDataService = unsubmittedCaseDataService;
    }

    @Override
    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(String id) {
        SavingMidEventDecorator savingMidEventDecorator = new SavingMidEventDecorator();
        return super.page(id, savingMidEventDecorator::handleMidEvent);
    }

    @Override
    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(
        String id, MidEvent<PCSCase, State> midEventCallback) {

        SavingMidEventDecorator savingMidEventDecorator = new SavingMidEventDecorator(midEventCallback);
        return super.page(id, savingMidEventDecorator::handleMidEvent);
    }


    private class SavingMidEventDecorator {

        private final MidEvent<PCSCase, State> wrappedMidEvent;

        public SavingMidEventDecorator() {
            this.wrappedMidEvent = null;
        }

        public SavingMidEventDecorator(MidEvent<PCSCase, State> midEvent) {
            this.wrappedMidEvent = midEvent;
        }

        public AboutToStartOrSubmitResponse<PCSCase, State> handleMidEvent(CaseDetails<PCSCase, State> details,
                                                                           CaseDetails<PCSCase, State> detailsBefore) {

            AboutToStartOrSubmitResponse<PCSCase, State> wrappedMidEventResponse = null;
            if (wrappedMidEvent != null) {
                wrappedMidEventResponse = wrappedMidEvent.handle(details, detailsBefore);
            }

            saveUnsubmittedData(details);

            return Optional.ofNullable(wrappedMidEventResponse)
                .orElseGet(() -> AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .data(details.getData())
                    .build());
        }

        private void saveUnsubmittedData(CaseDetails<PCSCase, State> details) {
            long caseReference = details.getId();
            PCSCase caseData = details.getData();

            unsubmittedCaseDataService.saveUnsubmittedCaseData(caseReference, caseData);
        }

    }

}
