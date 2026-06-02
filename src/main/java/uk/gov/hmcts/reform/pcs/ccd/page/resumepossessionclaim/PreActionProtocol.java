package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.PRE_ACTION_PROTOCOL_INCOMPLETE_EXPLANATION_LABEL;

@AllArgsConstructor
@Component
public class PreActionProtocol implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("preActionProtocol", this::midEvent)
                .pageLabel("Pre-action protocol")
                .label("preActionProtocol-info-england",
                        """
                  ---
                  <section tabindex="0">
                    <p class="govuk-body govuk-!-margin-bottom-3">
                    Registered providers of social housing should follow the pre-action protocol
                    before making a housing possession claim. You should have:
                    </p>

                    <ul class="govuk-list govuk-list--bullet govuk-!-margin-bottom-3">
                        <li class="govuk-!-font-size-19">contacted, or attempted to contact, the defendants</li>
                    </ul>

                    <p class="govuk-body govuk-!-margin-bottom-3">
                        If your claim is on the grounds of rent arrears, you should have:
                    </p>

                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19 govuk-!-margin-bottom-3">tried to agree a repayment plan</li>
                        <li class="govuk-!-font-size-19 govuk-!-margin-bottom-3">applied for arrears to be paid by the
                            Department for Work and Pensions (DWP) by deductions from the defendants’ benefits</li>
                        <li class="govuk-!-font-size-19 govuk-!-margin-bottom-3">
                            offered to assist the defendants in a claim for housing benefit or Universal Credit
                        </li>
                    </ul>

                    <div class="govuk-warning-text">
                        <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                        <strong class="govuk-warning-text__text">
                            <span class="govuk-visually-hidden">Warning</span>
                            Your case could be delayed or rejected if you have not followed
                            the pre-action protocol and completed all the steps.
                        </strong>
                    </div>
                  </section>

                  """,
                        "legislativeCountry=\"England\"")
                .label("preActionProtocol-info-wales",
                        """
                  ---
                  <section tabindex="0">
                    <p class="govuk-body govuk-!-margin-bottom-3">
                    Community landlords should follow the pre-action protocol
                    before making a housing possession claim. You should have:
                    </p>

                    <ul class="govuk-list govuk-list--bullet govuk-!-margin-bottom-3">
                        <li class="govuk-!-font-size-19">contacted, or attempted to contact, the defendants</li>
                    </ul>

                    <p class="govuk-body govuk-!-margin-bottom-3">
                        If your claim is on the grounds of rent arrears, you should have:
                    </p>

                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19 govuk-!-margin-bottom-3">tried to agree a repayment plan</li>
                        <li class="govuk-!-font-size-19 govuk-!-margin-bottom-3">applied for arrears to be paid by the
                        Department for Work and Pensions (DWP) by deductions from the defendants’ benefits</li>
                        <li class="govuk-!-font-size-19 govuk-!-margin-bottom-3">
                            offered to assist the defendants in a claim for housing benefit or Universal Credit
                        </li>
                    </ul>

                    <div class="govuk-warning-text">
                        <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                        <strong class="govuk-warning-text__text">
                            <span class="govuk-visually-hidden">Warning</span>
                            Your case could be delayed or rejected if you have not followed
                            the pre-action protocol and completed all the steps.
                        </strong>
                    </div>
                  </section>

                  """,
                        "legislativeCountry=\"Wales\"")
                .mandatoryWithLabel(PCSCase::getPreActionProtocolCompleted,
                        "Have you followed the pre-action protocol?")
                .mandatory(PCSCase::getPreActionProtocolIncompleteExplanation,
                           "legislativeCountry=\"England\" AND "
                               + "preActionProtocolCompleted=\"NO\"")
                .label("preActionProtocol-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
            caseData.getPreActionProtocolIncompleteExplanation(),
            PRE_ACTION_PROTOCOL_INCOMPLETE_EXPLANATION_LABEL,
            TextAreaValidationService.SHORT_TEXT_LIMIT
        );

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
