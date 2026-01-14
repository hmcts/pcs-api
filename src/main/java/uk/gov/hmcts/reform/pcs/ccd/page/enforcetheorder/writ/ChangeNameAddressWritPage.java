package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ChangeNameAddressWritPage implements CcdPageConfiguration {

    private static final String ERROR_MESSAGE =
            "You cannot continue with this application until you ask the judge for permission "
                    + "to change the name and address.";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("changeNameAddressWrit", this::midEvent)
            .pageLabel("You need permission from a judge to change the name and address for the eviction")
            .showCondition("writShowChangeNameAddressPage=\"Yes\" AND selectEnforcementType=\"WRIT\"")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .readonly(WritDetails::getShowChangeNameAddressPage, NEVER_SHOW)
            .done()
            .done()
            .label("changeNameAddressWrit-line-separator", "---")
            .label(
                "changeNameAddressWrit-information",
                """
                <p class="govuk-body">You need to ask permission from the judge before you can change the name or
                    address for the eviction.</p>
                <p class="govuk-body govuk-!-margin-bottom-0">The judge will decide if you:</p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-!-font-size-19">can change the address and continue with this application
                        (if the change is something small, like a typing error)</li>
                    <li class="govuk-!-font-size-19">must start again, with a new application
                        (if the change is more significant, like a completely different address)</li>
                </ul>
                <p class="govuk-body">You cannot continue with your application until you have asked the judge for
                    permission to make this change.</p>
                <p class="govuk-body govuk-!-font-weight-bold">If you want to proceed with changing the name or
                    address for the eviction</p>
                <p class="govuk-body">
                    <a id="changeNameAddress-link" class="govuk-link">
                        Ask the judge if you can change the name or address for the eviction
                        (GOV.UK, opens in a new tab)</a>.
                </p>
                <p class="govuk-body govuk-!-font-weight-bold">If you want to keep the existing name or address</p>
                <p class="govuk-body">If you are confident that the name or address is correct, you can go back to the
                    previous page and change your answer to the last question.</p>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-warning-text__assistive">Warning</span>
                    If the name and address is incorrect, the bailiff will not be able to carry out the eviction.
                  </strong>
                </div>
                """
            )
            .label("changeNameAddressWrit-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
            CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> before) {

        // Always return an error to block progression
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of(ERROR_MESSAGE))
                .build();
    }
}