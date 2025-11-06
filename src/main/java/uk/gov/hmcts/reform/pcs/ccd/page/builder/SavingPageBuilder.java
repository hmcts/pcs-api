package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.Optional;

public class SavingPageBuilder extends PageBuilder {

    private final DraftCaseDataService draftCaseDataService;
    
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> lastPageBuilder;
    private String lastPageId;

    public SavingPageBuilder(DraftCaseDataService draftCaseDataService,
                             EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        super(eventBuilder);
        this.draftCaseDataService = draftCaseDataService;
    }

    @Override
    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(String id) {
        return createPage(id, new SavingMidEventDecorator());
    }

    @Override
    public FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page(
        String id, MidEvent<PCSCase, State> midEventCallback) {
        return createPage(id, new SavingMidEventDecorator(midEventCallback));
    }

    @Override
    public SavingPageBuilder add(CcdPageConfiguration pageConfiguration) {
        clearPageTracking();
        super.add(pageConfiguration);
        addSaveAndReturnLabelIfPageTracked();
        return this;
    }

    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> createPage(
        String id, SavingMidEventDecorator savingMidEventDecorator) {
        FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> pageBuilder =
            super.page(id, savingMidEventDecorator::handleMidEvent);
        trackPage(pageBuilder, id);
        return pageBuilder;
    }

    private void trackPage(FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> pageBuilder,
                          String pageId) {
        this.lastPageBuilder = pageBuilder;
        this.lastPageId = pageId;
    }

    private void clearPageTracking() {
        lastPageBuilder = null;
        lastPageId = null;
    }

    private void addSaveAndReturnLabelIfPageTracked() {
        if (lastPageBuilder != null && lastPageId != null) {
            SaveAndReturnFieldCollectionBuilderWrapper.addSaveAndReturnLabel(lastPageBuilder, lastPageId);
            clearPageTracking();
        }
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

            patchUnsubmittedData(details);

            return Optional.ofNullable(wrappedMidEventResponse)
                .orElseGet(() -> AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .data(details.getData())
                    .build());
        }

        private void patchUnsubmittedData(CaseDetails<PCSCase, State> details) {
            long caseReference = details.getId();
            PCSCase caseData = details.getData();

            draftCaseDataService.patchUnsubmittedCaseData(caseReference, caseData);
        }

    }

}
