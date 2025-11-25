package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LandRegistryFees;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class LandRegistryFeesPage implements CcdPageConfiguration {

    static final String VALID_AMOUNT_ERROR_MESSAGE = "Please enter a valid amount of Land Registry fees";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFeesPage", this::midEvent)
                .pageLabel("Land Registry fees")
                .label("landRegistryFeesPage-content", "---")
                .label(
                    "landRegistryFeesPage-information-text", """
                        <p class="govuk-body govuk-!-font-weight-bold">
                           Have you paid any Land Registry fees?
                        </p>
                        """)
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getLandRegistryFees)
                .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
                .mandatory(
                    LandRegistryFees::getAmountOfLandRegistryFees,
                    "haveLandRegistryFeesBeenPaid=\"YES\"")
                .done()
                .label("landRegistryFeesPage-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(validateUserInput(data))
            .build();
    }

    List<String> validateUserInput(PCSCase data) {
        List<String> errors = new ArrayList<>();
        LandRegistryFees landRegistryFees = data.getEnforcementOrder().getLandRegistryFees();
        if (VerticalYesNo.YES == landRegistryFees.getHaveLandRegistryFeesBeenPaid()) {
            String amountOfLegalCosts = landRegistryFees.getAmountOfLandRegistryFees();
            if (!isValidLegalCostsAmount(amountOfLegalCosts)) {
                errors.add(VALID_AMOUNT_ERROR_MESSAGE);
            }
        }
        return errors;
    }

    private boolean isValidLegalCostsAmount(String amountOfLegalCosts) {
        if (amountOfLegalCosts == null || amountOfLegalCosts.isBlank()) {
            return false;
        }
        try {
            BigDecimal amount = new BigDecimal(amountOfLegalCosts);
            return amount.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
