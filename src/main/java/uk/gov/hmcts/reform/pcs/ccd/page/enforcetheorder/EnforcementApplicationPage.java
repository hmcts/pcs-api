package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantOfRestitutionMapper;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestSupportEnvironment;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WRIT_FLOW;

@Component
@AllArgsConstructor
public class EnforcementApplicationPage implements CcdPageConfiguration {

    private final EnforcementOrderService enforcementOrderService;
    private final WarrantOfRestitutionMapper warrantOfRestitutionMapper;

    private static final String STUB_GA_SUCCESSFUL_CONDITION =
        WRIT_FLOW + " AND writHasClaimTransferredToHighCourt=\"Yes\"";
    private static final String ERROR_GA_TRANSFER_UNSUCCESSFUL =
        "You cannot continue with this application because your "
            + "application to transfer to the High Court was unsuccessful";

    public static final String ENFORCEMENT_TYPES_INFORMATION = """
                    <details class="govuk-details">
                        <summary class="govuk-details__summary">
                            <span class="govuk-details__summary-text">
                                I do not know if I need a writ or a warrant
                            </span>
                        </summary>
                        <div class="govuk-details__text">
                            <p class="govuk-body">You can use either a warrant or a writ to evict someone. You cannot
                            apply for a warrant if you have already transferred enforcement of the possession order
                            to the High Court.</p>
                            <p class="govuk-body govuk-!-font-weight-bold">If you choose a warrant of possession</p>
                            <p class="govuk-body govuk-!-margin-bottom-1">If you apply for a warrant:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19">it costs ${warrantFeeAmount} to apply</li>
                              <li class="govuk-!-font-size-19">it’s free to hire a County Court bailiff</li>
                            </ul>
                            <p class="govuk-body govuk-!-font-weight-bold">If you ask the judge for a writ of
                            possession</p>
                            <p class="govuk-body govuk-!-margin-bottom-4"> You’ll need to:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19">apply to transfer enforcement of the possession order to
                              the High Court. This costs £126 and you'll need to provide a reason why you're
                              requesting to transfer</li>
                              <li class="govuk-!-font-size-19">ask the judge for a writ
                              (you'll need additional evidence for this
                              and the judge could still refuse your request)</li>
                              <li class="govuk-!-font-size-19">hire a private High Court Enforcement Officer
                              to carry out the eviction</li>
                            </ul>
                            <p class="govuk-body govuk-!-margin-bottom-4">If you apply for a writ:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19">it costs ${writFeeAmount} to apply</li>
                              <li class="govuk-!-font-size-19"><a href="https://www.hceoa.org.uk/choosing-a-hceo"
                              rel="noreferrer noopener"
                              target="_blank" class="govuk-link">you can get a quote from a bailiff to find out how
                              much it will cost(opens in a new tab, High Court Enforcement Officers Association
                              website)</a> (they usually charge by the hour)</li>
                            </ul>
                            <p class="govuk-body govuk-!-font-weight-bold">The additional evidence you’ll need to
                            apply for a writ</p>
                            <p class="govuk-body">You'll need to provide reasons for applying for a writ rather
                            than a warrant.</p>
                            <p class="govuk-body">The judge will consider this evidence, along with the other
                             information in your application, when deciding whether to grant your request.</p>
                            <p class="govuk-body">You can Contact a lawyer or a High Court Enforcement Officer
                            before you apply for a writ. They can help you to check if you have the evidence to apply
                            successfully.</p>
                            ${warrantOfRestitutionInfoText} </div>
                    </details>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enforcementApplication", this::midEvent)
            .pageLabel("Your application")
            .label("enforcementApplication-content", "---")
            .readonly(PCSCase::getFormattedDefendantNames, NEVER_SHOW, true)
            .readonly(PCSCase::getFormattedPropertyAddress, NEVER_SHOW, true)
            .complex(PCSCase::getEnforcementOrder)
            .mandatory(EnforcementOrder::getChooseEnforcementType)
            .readonly(EnforcementOrder::getWarrantFeeAmount, NEVER_SHOW, true)
            .readonly(EnforcementOrder::getWritFeeAmount, NEVER_SHOW, true)
            .readonly(EnforcementOrder::getWarrantOfRestitutionInfoText, NEVER_SHOW, true)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(
                WritDetails::getHasClaimTransferredToHighCourt,
                claimTransferredShowCondition()
            )
            .mandatory(
                WritDetails::getWasGeneralApplicationToTransferToHighCourtSuccessful,
                gaSuccessfulShowCondition()
            )
            .done()
            .done()
            .label("enforcementApplication-clarification", ENFORCEMENT_TYPES_INFORMATION)
            .label("enforcementApplication-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        setFormattedDefendantNames(data.getAllDefendants(), data);
        List<String> errors = validateWritTransfer(data);
        EnforcementOrder enforcementOrder = data.getEnforcementOrder();
        if ((SelectEnforcementType.WARRANT_OF_RESTITUTION).name()
                .equals(enforcementOrder.getChooseEnforcementType().getValueCode())) {
            populateWarrantRestDetails(enforcementOrder, details.getId());
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    private void setFormattedDefendantNames(List<ListValue<Party>> defendants, PCSCase pcsCase) {
        if (defendants != null && !defendants.isEmpty()) {
            pcsCase.setFormattedDefendantNames(defendants.stream()
                .map(defendant ->
                    defendant.getValue().getFirstName() + " " + defendant.getValue().getLastName()
                    + "<br>")
                .collect(Collectors.joining("\n")));
        }
    }

    private static boolean toBoolean(YesOrNo yesOrNo) {
        return yesOrNo != null && yesOrNo.toBoolean();
    }

    private List<String> validateWritTransfer(PCSCase pcsCase) {
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        if (SelectEnforcementType.getSelectEnforcementTypeFromName(
                enforcementOrder.getChooseEnforcementType().getValueCode()) != SelectEnforcementType.WRIT) {
            return List.of();
        }

        WritDetails writDetails = enforcementOrder.getWritDetails();
        boolean claimTransferred = toBoolean(writDetails.getHasClaimTransferredToHighCourt());
        boolean gaSuccessful = toBoolean(writDetails.getWasGeneralApplicationToTransferToHighCourtSuccessful());

        if (claimTransferred && !gaSuccessful) {
            return List.of(ERROR_GA_TRANSFER_UNSUCCESSFUL);
        }

        return List.of();
    }

    private String claimTransferredShowCondition() {
        return TestSupportEnvironment.isNonProdTestSupportEnabled() ? WRIT_FLOW : NEVER_SHOW;
    }

    private String gaSuccessfulShowCondition() {
        return TestSupportEnvironment.isNonProdTestSupportEnabled()
            ? STUB_GA_SUCCESSFUL_CONDITION
            : NEVER_SHOW;
    }

    private void populateWarrantRestDetails(EnforcementOrder currentEnforcementOrder, long caseReference) {
        EnforcementOrder warrantEnforcementOrder =
                enforcementOrderService.retrieveEnforcementOrder(caseReference, SelectEnforcementType.WARRANT);
        if (warrantEnforcementOrder != null) {
            warrantOfRestitutionMapper.prePopulateFieldsFromWarrantDetails(
                    warrantEnforcementOrder, currentEnforcementOrder);
        }
    }
}
