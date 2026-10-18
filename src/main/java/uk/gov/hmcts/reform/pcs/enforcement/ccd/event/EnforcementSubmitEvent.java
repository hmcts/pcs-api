package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.LiveEnforcementExistsException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;

/**
 * Submits the enforcement application for "Enforce the order" and "Resume draft application": it writes the
 * application to enf_case, deletes the draft and moves the claim to pending enforcement issued. When the
 * application owes a fee, a Payments service request is raised for it and the confirmation screen sends the
 * claimant to the case's Service Request tab to pay, as the claim issue and general application fees do.
 */
@Component("enforcementSubmitEventHandler")
@Slf4j
@RequiredArgsConstructor
public class EnforcementSubmitEvent implements Submit<PCSCase, State> {

    static final String NO_ENFORCEMENT_TYPE_ERROR =
        "Select what you want to apply for before submitting this application";
    static final String FEE_UNAVAILABLE_ERROR =
        "We could not get the fee for this application. Try submitting it again later";

    private final EnforcementOrderService enforcementOrderService;
    private final MoneyFormatter moneyFormatter;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        long caseReference = payload.caseReference();
        PCSCase pcsCase = payload.caseData();
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();

        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcementOrder);
        if (enforcementType == null) {
            return errorResponse(NO_ENFORCEMENT_TYPE_ERROR);
        }
        // Drafts are per user and organisation, so another claimant-side user may have submitted their own
        // draft since this one was started. A claim can only have one live enforcement.
        if (enforcementOrderService.hasLiveEnforcement(caseReference)) {
            return errorResponse(LiveEnforcementExistsException.MESSAGE);
        }

        Optional<FeeDetails> feeOwed;
        try {
            feeOwed = enforcementOrderService.saveAndClearDraftData(caseReference, enforcementOrder);
        } catch (FeeNotFoundException e) {
            log.error("Could not look up the {} fee to submit the enforcement on case {}",
                      enforcementType, caseReference, e);
            return errorResponse(FEE_UNAVAILABLE_ERROR);
        } catch (DataIntegrityViolationException e) {
            if (!LiveEnforcementExistsException.isConstraintViolation(e)) {
                throw e;
            }
            log.info("Case {}: another live enforcement was created while this application was being submitted",
                     caseReference);
            return errorResponse(LiveEnforcementExistsException.MESSAGE);
        }
        log.debug("Saved submitted enforcement order data and deleted draft data for case reference {}", caseReference);
        // An application that owes nothing (restitution, or a free repeat warrant) gets no payment panel.
        return SubmitResponse.<State>builder()
            .confirmationBody(feeOwed
                                  .map(fee -> getPaymentConfirmationMarkdown(
                                      moneyFormatter.formatFee(fee.getFeeAmount()), caseReference))
                                  .orElse(null))
            .state(PENDING_ENFORCEMENT_ISSUED)
            .build();
    }

    private static SubmitResponse<State> errorResponse(String error) {
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }

    private static String getPaymentConfirmationMarkdown(String enforcementFee, long caseReference) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Pay %s for HM Courts and Tribunals Service to
            review your application</span>
            </div>

            <div class="govuk-body">
            We have saved your application, but you still need to pay for it before we can review it.
            This costs %s
            </div>

            <div class="govuk-body">
            We cannot review your application until you pay this fee.
            </div>

            <div class="govuk-body">
            <a href="/cases/case-details/%d#Service%%20Request"
                    class="govuk-link govuk-link--no-visited-state">Pay your application fee</a>.
            </div>
            """.formatted(enforcementFee, enforcementFee, caseReference);
    }

}
