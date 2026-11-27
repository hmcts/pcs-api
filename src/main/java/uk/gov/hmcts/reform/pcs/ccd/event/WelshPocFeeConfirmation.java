package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.welshPocFeeConfirmation;

/**
 * Proof of concept event for a Welsh translation feature request. Not for merge.
 *
 * <p>ExUI fills in {@code ${...}} placeholders in a Label on an event page, translating the template first. It does
 * not do this for the confirmation header or body: the confirmation page renders each as one phrase with
 * {@code | rpxTranslate} and never fills anything in. So a fee confirmation has to be filled in by the service, and
 * the result, with its case number in it, never matches a dictionary row.
 *
 * <p>The page shows a Label using {@code ${welshPocFeeAmount}}. The confirmation header uses the same placeholder
 * and {@code ${[CASE_REFERENCE]}}. The body is the same sentence filled in by pcs-api, which is today's approach.
 * Nothing is paid, scheduled or saved and the state does not change, so the real fee screens are untouched.
 *
 * <p>Every phrase starts with {@code TEST-}, because that is the only prefix ts-translation-service can delete.
 */
@Component
public class WelshPocFeeConfirmation implements CCDConfig<PCSCase, State, UserRole> {

    static final String FEE_IN_PENCE = "40400";
    static final String FORMATTED_FEE = "£404.00";

    static final String PAGE_LABEL = "TEST- The fee is ${welshPocFeeAmount}. Submit to see the same placeholder"
        + " in the confirmation header.";
    static final String CONFIRMATION_HEADER = "TEST- Pay ${welshPocFeeAmount} fee for case ${[CASE_REFERENCE]}";
    static final String CONFIRMATION_BODY = "TEST- Pay %s fee for case %d";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(welshPocFeeConfirmation.name(), this::submit, this::start)
                .forAllStates()
                // Definition store caps event names at 30 characters.
                .name("TEST- Welsh fee PoC")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR, UserRole.GA_CLAIMANT_SOLICITOR, UserRole.CLAIMANT)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .endButtonLabel("Submit");

        new PageBuilder(eventBuilder)
            .page("welshPocFeeConfirmation")
            .pageLabel("TEST- Welsh fee confirmation proof of concept")
            .readonly(PCSCase::getWelshPocFeeAmount, NEVER_SHOW, true)
            .label("welshPocFeeLabel", PAGE_LABEL);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setWelshPocFeeAmount(FEE_IN_PENCE);
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.<State>builder()
            .confirmationHeader(CONFIRMATION_HEADER)
            .confirmationBody(CONFIRMATION_BODY.formatted(FORMATTED_FEE, eventPayload.caseReference()))
            .build();
    }
}
