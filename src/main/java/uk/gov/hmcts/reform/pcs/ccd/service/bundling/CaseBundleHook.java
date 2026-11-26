package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseEventContext;
import uk.gov.hmcts.ccd.sdk.CaseEventHook;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocJobs;

/**
 * Submits the case's bundle after every case event, in the event's transaction. The doc toolkit renders it only when
 * it changed, so PCS never works out which changes affect the bundle.
 */
@Component
@RequiredArgsConstructor
public class CaseBundleHook implements CaseEventHook {

    private final CaseBundleDefinition bundle;
    private final ObjectProvider<DocJobs> jobs;

    @Override
    public void afterCaseEvent(CaseEventContext event) {
        DocJobs docJobs = jobs.getIfAvailable();
        if (docJobs == null) {
            return;
        }
        bundle.build(event.caseReference()).ifPresent(request ->
            docJobs.submit(event.caseReference(), CaseBundleDefinition.NAME, request));
    }
}
