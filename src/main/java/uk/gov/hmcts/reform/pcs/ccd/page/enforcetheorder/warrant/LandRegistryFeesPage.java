package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class LandRegistryFeesPage implements CcdPageConfiguration {

    private final MoneyConverter moneyConverter;
    private final RepaymentTableRenderer repaymentTableRenderer;
    private final FeeValidationService feeValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("landRegistryFees", this::midEvent)
            .pageLabel("Land Registry fees")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
            .label("landRegistryFees-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getLandRegistryFees)
            .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
            .mandatory(LandRegistryFees::getAmountOfLandRegistryFees, "warrantHaveLandRegistryFeesBeenPaid=\"YES\"")
            .done()
            .done()
            .done()
            .label("landRegistryFees-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        if (!validationErrors.isEmpty()) {

            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(validationErrors)
                .build();
        }

        WarrantDetails warrantDetails = caseData.getEnforcementOrder().getWarrantDetails();

        BigDecimal totalArrears = warrantDetails.getMoneyOwedByDefendants()
            .getAmountOwed();
        BigDecimal landRegistryFee = warrantDetails.getLandRegistryFees()
            .getAmountOfLandRegistryFees();
        BigDecimal legalCosts = warrantDetails.getLegalCosts()
            .getAmountOfLegalCosts();
        BigDecimal warrantFeePence = convertWarrantFeeToBigDecimal(caseData);
        BigDecimal totalFees = getTotalFees(totalArrears, landRegistryFee, legalCosts, warrantFeePence);

        // Render repayment table for Repayments screen (default caption)
        // Some fee fields are legitimately null when the UI uses YES/NO toggles,
        // So default them to ZERO before rendering.
        String repaymentTableHtml = repaymentTableRenderer.render(
            totalArrears,
            legalCosts,
            landRegistryFee,
            caseData.getEnforcementOrder().getWarrantFeeAmount(),
            totalFees
        );

        // Render repayment table for SOT screen (custom caption)
        String statementOfTruthRepaymentTableHtml = repaymentTableRenderer.render(
            totalArrears,
            legalCosts,
            landRegistryFee,
            caseData.getEnforcementOrder().getWarrantFeeAmount(),
            totalFees,
            "The payments due"
        );

        RepaymentCosts repaymentCosts = warrantDetails.getRepaymentCosts();
        repaymentCosts.setRepaymentSummaryMarkdown(repaymentTableHtml);
        repaymentCosts.setStatementOfTruthRepaymentSummaryMarkdown(statementOfTruthRepaymentTableHtml);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        List<String> errors = new ArrayList<>();
        LandRegistryFees landRegistryFees = caseData.getEnforcementOrder()
            .getWarrantDetails().getLandRegistryFees();

        if (landRegistryFees.getHaveLandRegistryFeesBeenPaid().toBoolean()) {
            errors.addAll(feeValidationService.validateFee(
                landRegistryFees.getAmountOfLandRegistryFees(),
                "Land Registry fees"
            ));
        }
        return errors;
    }

    private BigDecimal convertWarrantFeeToBigDecimal(PCSCase caseData) {
        String warrantFee = moneyConverter.convertPoundsToPence(caseData.getEnforcementOrder().getWarrantFeeAmount());
        return moneyConverter.convertPenceToBigDecimal(warrantFee);
    }

    private BigDecimal getTotalFees(BigDecimal... fees) {
        return Arrays.stream(fees)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
