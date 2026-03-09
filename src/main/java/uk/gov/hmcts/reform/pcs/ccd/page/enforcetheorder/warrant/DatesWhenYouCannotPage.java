package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.DateOfEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Error page shown when user indicates the name or address for eviction is incorrect.
 * This page blocks progression and requires the user to make a general application.
 */
public class DatesWhenYouCannotPage implements CcdPageConfiguration {

    private static final String ERROR_MESSAGE =
        "You cannot continue with this application until you ask the judge for permission "
        + "to change the name and address.";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("datesWhenYouCannotPage", this::midEvent)
            .pageLabel("Dates when you cannot attend an eviction")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW
                    + " AND warrantShowChangeNameAddressPage=\"Yes\"")
            .label("datesWhenYouCannotPage-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getDoe)
            .mandatory(DateOfEviction::getDateOfEvictions)
            .mandatory(DateOfEviction::getAdditionalDates,"warrantDateOfEvictions=\"YES\"" )
            .done()
            .done()
            .done()
            .label("datesWhenYouCannotPage-line-separator", "---");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        // Always return an error to block progression
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(ERROR_MESSAGE)
            .build();
    }
}

