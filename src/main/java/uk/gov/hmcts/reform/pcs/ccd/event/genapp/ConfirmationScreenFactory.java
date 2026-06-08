package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppFeeCalculator;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ConfirmationScreenFactory {

    private final GenAppFeeCalculator genAppFeeCalculator;
    private final MoneyFormatter moneyFormatter;

    public SubmitResponse<State> buildConfirmationScreenResponse(GenAppRequest genAppRequest,
                                                                 long caseReference) {

        if (!isXuiJourney(genAppRequest)) {
            return SubmitResponse.<State>builder().build();
        }

        String confirmationMarkdown = genAppFeeCalculator.getApplicationFee(genAppRequest)
            .map(feeAmount -> buildFeeConfirmationMarkdown(feeAmount, caseReference))
            .orElseGet(() -> buildNoFeeConfirmationMarkdown(genAppRequest));

        return SubmitResponse.<State>builder()
            .confirmationBody(confirmationMarkdown)
            .build();
    }

    private static boolean isXuiJourney(GenAppRequest genAppRequest) {
        return genAppRequest instanceof XuiGenAppRequest;
    }

    private String buildFeeConfirmationMarkdown(BigDecimal feeAmount, long caseReference) {
        String formattedFee = moneyFormatter.formatFee(feeAmount);

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Pay %s application fee</span>
            </div>

            <p class="govuk-body govuk-!-padding-top-6">You must pay the application fee of %s.</p>

            <p class="govuk-body">Your application will not progress until this fee has been paid.</p>

            <p class="govuk-body">
            <a href="/cases/case-details/%d#Service%%20Request"
                    class="govuk-link govuk-link--no-visited-state">Pay your application fee</a>
            </p>
            """.formatted(formattedFee, formattedFee, caseReference);
    }

    private String buildNoFeeConfirmationMarkdown(GenAppRequest genAppRequest) {
        GenAppType applicationType = genAppRequest.getApplicationType();

        String receivedRequestMessage = switch (applicationType) {
            case ADJOURN -> "We have received your request to adjourn (delay) the court hearing.";
            case SET_ASIDE -> "We have received your request to set aside (cancel) the order.";
            case SOMETHING_ELSE -> "We have received your request to ask the court to make an order.";
        };

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Application submitted</span>
            </div>

            <p class="govuk-body govuk-!-padding-top-6">%s</p>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body">
            You do not need to do anything else. We will send the defendant’s application to the judge and we will
            contact you again when they have made a decision.
            </p>

            <h3 class="govuk-heading-s">Give feedback about this service</h3>
            <p class="govuk-body">
            <a href="https://www.smartsurvey.co.uk/s/CCDSurvey/"
                  rel="noreferrer noopener"
                  target="_blank"
                  class="govuk-link">Complete this short survey to help us improve this service</a>
            (usually takes 5 minutes).
            </p>
            """.formatted(receivedRequestMessage);

    }

}
